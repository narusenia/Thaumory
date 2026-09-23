package one.nxeu.thaumory.pipe;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.block.core.CoreBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.block.pipe.EssentiaPipeBlock;
import one.nxeu.thaumory.block.pipe.PipeBlockEntity;
import one.nxeu.thaumory.essentia.EssentiaHandle;

/**
 * The pipe networks of one level (requirements §8.2). A network is worked out once, when its
 * pipes or what they touch change, and then steps on an interval. Only loaded pipes take part.
 */
public final class PipeNetworks {
    private static final Map<ServerLevel, PipeNetworks> LEVELS = new WeakHashMap<>();
    private static volatile PipeSettings settings = PipeSettings.DEFAULT;

    private final ServerLevel level;
    private final Map<BlockPos, Network> byPipe = new HashMap<>();
    private final Set<BlockPos> dirty = new HashSet<>();

    private PipeNetworks(ServerLevel level) {
        this.level = level;
    }

    public static PipeNetworks of(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, PipeNetworks::new);
    }

    public static void updateSettings(PipeSettings newSettings) {
        settings = newSettings;
    }

    public static void register() {
        TickEvent.SERVER_LEVEL_POST.register(level -> {
            PipeNetworks networks = LEVELS.get(level);
            if (networks != null) {
                networks.tick();
            }
        });
        // After the last save, which asks the networks for each pipe's share.
        LifecycleEvent.SERVER_STOPPED.register(server -> LEVELS.clear());
    }

    /** The pipe at {@code pos}, or something next to it, changed: its network is worked out again. */
    public void invalidate(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    /** A pipe's block entity is going (broken, or its chunk unloading). */
    public void removed(BlockPos pos, PipeBlockEntity pipe) {
        invalidate(pos);
    }

    /** This pipe's share of what its network carries, while it is part of one. */
    public Optional<AspectList> shareOf(BlockPos pos) {
        Network network = byPipe.get(pos);
        return network == null ? Optional.empty() : Optional.of(network.shares().get(network.index(pos)));
    }

    private void tick() {
        if (!dirty.isEmpty()) {
            rebuild();
        }
        long time = level.getGameTime();
        for (Network network : Set.copyOf(byPipe.values())) {
            if (Math.floorMod(time + network.origin().hashCode(), settings.interval()) == 0) {
                network.step();
            }
        }
    }

    private void rebuild() {
        Set<BlockPos> touched = new HashSet<>();
        for (BlockPos pos : dirty) {
            touched.add(pos);
            for (Direction side : Direction.values()) {
                touched.add(pos.relative(side));
            }
        }
        dirty.clear();
        for (BlockPos pos : touched) {
            Network network = byPipe.get(pos);
            if (network != null) {
                dissolve(network);
            }
        }
        for (BlockPos pos : touched) {
            if (!byPipe.containsKey(pos) && pipeAt(pos).isPresent()) {
                build(pos);
            }
        }
    }

    private Optional<PipeBlockEntity> pipeAt(BlockPos pos) {
        if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof EssentiaPipeBlock)) {
            return Optional.empty();
        }
        return level.getBlockEntity(pos) instanceof PipeBlockEntity pipe && !pipe.isRemoved() ? Optional.of(pipe) : Optional.empty();
    }

    /** Hands each pipe still there its share. A pipe gone takes its share with it. */
    private void dissolve(Network network) {
        List<AspectList> shares = network.shares();
        for (int i = 0; i < network.pipes.size(); i++) {
            BlockPos pos = network.pipes.get(i);
            byPipe.remove(pos);
            int index = i;
            pipeAt(pos).ifPresent(pipe -> pipe.setShare(shares.get(index)));
        }
    }

    private void build(BlockPos start) {
        List<BlockPos> pipes = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(List.of(start));
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        Map<BlockPos, Direction> ends = new LinkedHashMap<>();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            pipes.add(pos);
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (pipeAt(next).isPresent()) {
                    if (seen.add(next)) {
                        queue.add(next);
                    }
                } else if (!ends.containsKey(next) && EssentiaPipeBlock.joins(level, pos, side)) {
                    ends.put(next, side.getOpposite());
                }
            }
        }
        pipes.sort(Comparator.naturalOrder());
        AspectList buffer = AspectList.empty();
        for (BlockPos pos : pipes) {
            PipeBlockEntity pipe = pipeAt(pos).orElseThrow();
            buffer = buffer.plus(pipe.share());
        }
        List<Endpoint> endpoints = new ArrayList<>();
        ends.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(end -> Thaumory.essentia()
                .find(level, end.getKey(), end.getValue())
                .ifPresent(handle -> endpoints.add(new Endpoint(end.getKey(), handle))));
        Network network = new Network(List.copyOf(pipes), List.copyOf(endpoints), buffer);
        pipes.forEach(pos -> byPipe.put(pos, network));
    }

    /** A container a network touches, with its priority read afresh each step (a jar's label can change). */
    private final class Endpoint implements PipeEndpoint {
        private final BlockPos pos;
        private final EssentiaHandle handle;

        Endpoint(BlockPos pos, EssentiaHandle handle) {
            this.pos = pos;
            this.handle = handle;
        }

        @Override
        public int priority() {
            return switch (level.getBlockEntity(pos)) {
                case CoreBlockEntity core -> 3;
                case JarBlockEntity jar when jar.contents().label().isPresent() -> 2;
                case null, default -> 1;
            };
        }

        @Override
        public AspectList contents() {
            return handle.contents();
        }

        @Override
        public int space(Aspect aspect) {
            return handle.space(aspect);
        }

        @Override
        public int insert(Aspect aspect, int max) {
            return handle.insert(aspect, max);
        }

        @Override
        public int extract(Aspect aspect, int max) {
            return handle.extract(aspect, max);
        }
    }

    private final class Network {
        /** Sorted, so shares always go to the same pipes. */
        private final List<BlockPos> pipes;
        private final Map<BlockPos, Integer> indices = new HashMap<>();
        private final List<Endpoint> endpoints;
        private AspectList buffer;
        private List<AspectList> shares;

        Network(List<BlockPos> pipes, List<Endpoint> endpoints, AspectList buffer) {
            this.pipes = pipes;
            this.endpoints = endpoints;
            this.buffer = buffer;
            for (int i = 0; i < pipes.size(); i++) {
                indices.put(pipes.get(i), i);
            }
        }

        BlockPos origin() {
            return pipes.getFirst();
        }

        int index(BlockPos pos) {
            return indices.get(pos);
        }

        List<AspectList> shares() {
            if (shares == null) {
                shares = PipeBuffer.split(buffer, pipes.size());
            }
            return shares;
        }

        void step() {
            if (endpoints.isEmpty() && buffer.isEmpty()) {
                return;
            }
            AspectList next = PipeFlow.step(buffer, pipes.size() * settings.bufferPerPipe(), endpoints, settings);
            if (!next.equals(buffer)) {
                buffer = next;
                shares = null;
                // So the chunks save the new shares.
                pipes.forEach(pos -> pipeAt(pos).ifPresent(PipeBlockEntity::setChanged));
            }
        }
    }
}
