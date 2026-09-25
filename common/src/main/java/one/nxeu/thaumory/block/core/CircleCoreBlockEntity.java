package one.nxeu.thaumory.block.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.api.essentia.EssentiaContainer;
import one.nxeu.thaumory.api.flux.FluxStage;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.circle.CircleChildren;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.circle.CircleDefinitions;
import one.nxeu.thaumory.circle.CircleIndex;
import one.nxeu.thaumory.circle.CircleMode;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.circle.CirclePlane;
import one.nxeu.thaumory.circle.CircleSide;
import one.nxeu.thaumory.circle.CircleUpkeep;
import one.nxeu.thaumory.circle.InfusionRules;
import one.nxeu.thaumory.circle.InfusionSettings;
import one.nxeu.thaumory.circle.WorkFlux;
import one.nxeu.thaumory.flux.FluxSettings;
import one.nxeu.thaumory.flux.FluxWorldEffects;
import one.nxeu.thaumory.infusion.Infusion;
import one.nxeu.thaumory.infusion.InfusionCapacities;
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.particle.ThaumoryParticles;
import one.nxeu.thaumory.sound.ThaumorySounds;
import one.nxeu.thaumory.text.ThaumoryText;

/**
 * A magic circle's Core: up to three runes (four from rank 3), kept by aspect id in slot order, the
 * Essentia poured in for them, and the chalk around it as last scanned. The scan is not saved; it runs again every
 * {@code scan_interval} ticks and goes to clients when it changes, for the loupe.
 *
 * <p>A sustained circle, once started, pays its upkeep on its interval and works once a second
 * until it is stopped, runs dry, or its circle breaks.
 *
 * <p>Trying an undefined combination releases Flux and records the failure; every payment of an
 * unstable circle may release Flux too. Flux around the Core makes it less stable from
 * manifestation on, and at overload any payment may make it misfire and explode.
 *
 * <p>A combination that needs a higher rank than the Core's is found but does not run: it is
 * neither a misfire nor recorded.
 *
 * <p>A Core on a node of a circle of higher rank sits in it and reads no chalk of its own
 * (requirements §4.6). If the parent holds it as a child, its runes make a sub-circle that works
 * on the parent's rings: the parent's range, centred on the parent, and the parent's multipliers
 * and instability. Each child adds to the parent's instability.
 */
public final class CircleCoreBlockEntity extends BlockEntity {
    /** The most rune slots any Core has; {@link #slots()} is this Core's. */
    public static final int SLOTS = 4;
    private static final Codec<List<Identifier>> RUNES_CODEC = Identifier.CODEC.listOf(0, SLOTS);
    private static final Codec<AspectList> ESSENTIA_CODEC = AspectCodecs.aspectList(ThaumoryApi.aspects());
    private static final CircleScan UNSCANNED = new CircleScan(0, List.of(), List.of(), List.of());
    /** The combination setting for the Flux given off per piece of work (requirements §4.5). */
    private static final String WORK_FLUX = "work_flux";
    /** The scan as the loupe needs it on the client; sent with block updates, never saved. */
    private static final Codec<CircleScan.Node> NODE_CODEC = RecordCodecBuilder.create(n -> n.group(
            Codec.INT.fieldOf("ring").forGetter(CircleScan.Node::ring),
            Codec.STRING.xmap(CircleSide::valueOf, CircleSide::name).fieldOf("side").forGetter(CircleScan.Node::side),
            Identifier.CODEC.fieldOf("pattern").forGetter(CircleScan.Node::pattern)
    ).apply(n, CircleScan.Node::new));
    private static final Codec<CircleScan> SCAN_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("rings").forGetter(CircleScan::rings),
            NODE_CODEC.listOf().fieldOf("nodes").forGetter(CircleScan::nodes),
            RecordCodecBuilder.<CircleScan.Offset>create(o -> o.group(
                    Codec.INT.fieldOf("dx").forGetter(CircleScan.Offset::dx),
                    Codec.INT.fieldOf("dz").forGetter(CircleScan.Offset::dz)
            ).apply(o, CircleScan.Offset::new)).listOf().fieldOf("ignored").forGetter(CircleScan::ignoredModifiers),
            NODE_CODEC.listOf().optionalFieldOf("seats", List.of()).forGetter(CircleScan::seats)
    ).apply(i, CircleScan::new));
    private static volatile CircleSettings settings = CircleSettings.DEFAULT;

    /**
     * What the circle takes, as the loupe shows it: per activation for a triggered circle, or 1 of
     * each effect rune every {@code interval} ticks for a sustained one.
     */
    public record Upkeep(CircleMode mode, int cost, int interval) {
        static final Codec<Upkeep> CODEC = RecordCodecBuilder.create(i -> i.group(
                CircleMode.CODEC.fieldOf("mode").forGetter(Upkeep::mode),
                Codec.INT.fieldOf("cost").forGetter(Upkeep::cost),
                Codec.INT.fieldOf("interval").forGetter(Upkeep::interval)
        ).apply(i, Upkeep::new));
    }

    /**
     * {@code UNDEFINED}: fewer than two runes, so not a circle yet. {@code MISFIRED}: the combination is undefined.
     * {@code LOW_RANK}: the combination is defined, but needs a Core of higher rank. {@code OVERLOADED}: it paid,
     * but Flux at overload made it misfire and explode.
     */
    public enum StartResult { STARTED, ALREADY_RUNNING, NO_RINGS, UNDEFINED, MISFIRED, LOW_RANK, TRIGGERED_ONLY, NO_ESSENTIA, OVERLOADED }

    public enum InfuseResult {
        INFUSED, FAILED, NO_ITEM, NO_RINGS, UNDEFINED, MISFIRED, LOW_RANK, NOT_INFUSABLE, NO_CAPACITY, NO_ROOM, ACTIVE_TAKEN, NO_ESSENTIA
    }

    /**
     * @param infusion what went into the item when it did, or what would have when there was no room
     * @param used the item's capacity in use, not counting an effect being burnt in again
     * @param capacity the item's capacity
     */
    public record InfuseOutcome(InfuseResult result, Optional<Infusion> infusion, int used, int capacity) {
        static InfuseOutcome of(InfuseResult result) {
            return new InfuseOutcome(result, Optional.empty(), 0, 0);
        }
    }

    public enum TriggerResult { TRIGGERED, NO_RINGS, UNDEFINED, MISFIRED, LOW_RANK, SUSTAINED_ONLY, NO_TARGET, NO_ESSENTIA, OVERLOADED, STOPPED }

    private List<Identifier> runes = List.of();
    private AspectList essentia = AspectList.empty();
    private ItemStack pedestalItem = ItemStack.EMPTY;
    private CircleScan scan = UNSCANNED;
    private int instability;
    /** The Cores sitting on this circle's nodes, by position, as last scanned. Server only. */
    private Map<BlockPos, CircleChildren.Placed> seats = Map.of();
    /** The Core of higher rank this one sits in, as last scanned. Server only. */
    private Optional<BlockPos> parent = Optional.empty();
    /** The server's view, as last received. Only meaningful on the client. */
    private int clientThreshold = CircleSettings.DEFAULT.instabilityThreshold();
    private int clientCapacity = CircleSettings.DEFAULT.essentiaCapacity();
    private Optional<Upkeep> clientUpkeep = Optional.empty();
    private Optional<Identifier> clientEffect = Optional.empty();
    private boolean clientLowRank;
    private int clientChildren;
    private Optional<CircleChildren.Seat> clientSeat = Optional.empty();
    private Optional<CircleSide> clientSeatSide = Optional.empty();
    private int clientFrameRings;

    /** The running sustained circle's definition id, or empty when it is not running. */
    private Optional<Identifier> running = Optional.empty();
    /** The block event a triggered circle sends its watchers when it goes off, for the glowing emblem. */
    public static final int FLASH_EVENT = 1;
    /** Client only: the game time a triggered circle last went off. */
    private long lastFlash = Long.MIN_VALUE;
    private Identifier runningEffect;
    /** The runes the running circle started with; any change stops it. */
    private List<Identifier> runningRunes = List.of();
    private long nextPayment;
    private CompoundTag effectData = new CompoundTag();
    /** A triggered effect still carrying out its last activation, if any (the mining circle digging, say). */
    private Identifier workingEffect;
    /** Work Flux held back until it comes to a whole unit. */
    private double workFlux;

    public CircleCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.CIRCLE_CORE_ENTITY.get(), pos, state);
    }

    public static void updateSettings(CircleSettings newSettings) {
        settings = newSettings;
    }

    public static CircleSettings settings() {
        return settings;
    }

    /**
     * Rescans on the interval, and runs a sustained circle. The offset by position keeps many
     * Cores from scanning on the same tick.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CircleCoreBlockEntity core) {
        long time = level.getGameTime();
        // The scan is not saved, so a Core just loaded scans at once: a running circle would take
        // the missing rings for a broken circle and stop.
        if (core.scan == UNSCANNED || Math.floorMod(time + pos.hashCode(), settings.scanInterval()) == 0) {
            core.rescan();
        }
        if (core.running.isPresent() && level instanceof ServerLevel server) {
            core.runSustained(server, time);
        } else if (core.workingEffect != null && level instanceof ServerLevel server) {
            core.continueWork(server, time);
        }
    }

    // Scanning

    public void rescan() {
        if (level == null) {
            return;
        }
        Identifier line = BuiltInRegistries.BLOCK.getKey(ThaumoryBlocks.CHALK_LINE.get());
        Direction front = front();
        CircleScan previousScan = scan;
        int previousInstability = instability;
        Map<BlockPos, CircleChildren.Placed> previousSeats = seats;
        Optional<CircleChildren.Seat> previousSeat = seat();
        int previousFrameRings = frameRings();
        parent = findParent();
        // A Core sitting in another's circle is part of that circle and reads no chalk of its own.
        scan = parent.isPresent() ? CircleScan.NONE : CircleScan.scan((dx, dz) -> {
            BlockState state = level.getBlockState(worldPosition.offset(CirclePlane.offset(front, dx, dz)));
            // Only patterns and Cores on the Core's own face count.
            return (state.is(ChalkPatternBlock.PATTERNS) || state.getBlock() instanceof CircleCoreBlock) && ChalkPatternBlock.front(state) == front
                    ? Optional.of(BuiltInRegistries.BLOCK.getKey(state.getBlock())) : Optional.empty();
        }, line, id -> BuiltInRegistries.BLOCK.getValue(id) instanceof CircleCoreBlock, maxRings());
        seats = placeSeats();
        instability = parent.isPresent() ? parentCore().map(core -> core.instability).orElse(0)
                : settings.instability(scan.nodes(), children()) + (level instanceof ServerLevel server
                        ? Thaumory.flux().settings().effects().extraInstability(fluxStage(server)) : 0);
        if (!level.isClientSide()) {
            stopIfBroken();
            updateIndex();
            if (!scan.equals(previousScan) || instability != previousInstability || !seats.equals(previousSeats)
                    || !seat().equals(previousSeat) || frameRings() != previousFrameRings) {
                sync();
            }
        }
    }

    /**
     * The Core of higher rank, on the same face, that holds this one on a node of its circle, if
     * any. One that does not list this Core yet scans again now, as it may have scanned before this
     * Core was placed or loaded.
     */
    private Optional<BlockPos> findParent() {
        Direction front = front();
        for (CircleSide side : CircleSide.values()) {
            for (int ring = 1; ring <= CircleScan.MAX_RINGS; ring++) {
                BlockPos pos = worldPosition.offset(CirclePlane.offset(front, -side.nodeX(ring), -side.nodeZ(ring)));
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CircleCoreBlockEntity other
                        && other.rank() > rank() && other.front() == front) {
                    if (!other.seats.containsKey(worldPosition)) {
                        other.rescan();
                    }
                    if (other.seats.containsKey(worldPosition)) {
                        return Optional.of(pos);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Which of the Cores on the nodes sit in this circle, and how (requirements §4.6). */
    private Map<BlockPos, CircleChildren.Placed> placeSeats() {
        if (scan.seats().isEmpty()) {
            return Map.of();
        }
        List<CircleChildren.Candidate> candidates = scan.seats().stream()
                .map(node -> new CircleChildren.Candidate(node, level.getBlockState(nodePos(node)).getBlock() instanceof CircleCoreBlock core
                        ? core.rank() : Integer.MAX_VALUE))
                .toList();
        Map<BlockPos, CircleChildren.Placed> placed = new LinkedHashMap<>();
        for (CircleChildren.Placed seat : CircleChildren.place(candidates, scan.rings(), rank())) {
            placed.put(nodePos(seat.node()), seat);
        }
        return Map.copyOf(placed);
    }

    private BlockPos nodePos(CircleScan.Node node) {
        return worldPosition.offset(CirclePlane.offset(front(), node.side().nodeX(node.ring()), node.side().nodeZ(node.ring())));
    }

    /** How many sub-circles this circle holds; what was last received on the client. */
    public int children() {
        if (level != null && level.isClientSide()) {
            return clientChildren;
        }
        return (int) seats.values().stream().filter(seat -> seat.seat() == CircleChildren.Seat.CHILD).count();
    }

    /** How this Core sits in a circle of higher rank, if it does; what was last received on the client. */
    public Optional<CircleChildren.Seat> seat() {
        if (level != null && level.isClientSide()) {
            return clientSeat;
        }
        return placed().map(CircleChildren.Placed::seat);
    }

    /** Which side of its parent's circle this Core sits on, if it does; what was last received on the client. */
    public Optional<CircleSide> seatSide() {
        if (level != null && level.isClientSide()) {
            return clientSeatSide;
        }
        return placed().map(placed -> placed.node().side());
    }

    private Optional<CircleChildren.Placed> placed() {
        return parentCore().map(core -> core.seats.get(worldPosition));
    }

    /** The Core this one sits in, while that one still holds it. Server only. */
    private Optional<CircleCoreBlockEntity> parentCore() {
        return parent.filter(pos -> level != null && level.isLoaded(pos))
                .map(pos -> level.getBlockEntity(pos) instanceof CircleCoreBlockEntity core && core.seats.containsKey(worldPosition) ? core : null);
    }

    /**
     * The rings a circle runs on: its own, or for a sub-circle its parent's, with the parent's
     * position as the centre of the range, its rank for the strength and its instability.
     */
    private record Frame(BlockPos centre, CircleScan scan, int rank, int instability) {}

    /** Server only. A Core that sits in a circle without running as its child has no rings at all. */
    private Frame frame() {
        if (parent.isEmpty()) {
            return new Frame(worldPosition, scan, rank(), instability);
        }
        return parentCore().filter(core -> core.seats.get(worldPosition).seat() == CircleChildren.Seat.CHILD)
                .map(core -> new Frame(core.worldPosition, core.scan, core.rank(), core.instability))
                .orElseGet(() -> new Frame(worldPosition, CircleScan.NONE, rank(), instability));
    }

    /** The rings the circle runs on: its own, or its parent's for a sub-circle; what was last received on the client. */
    public int frameRings() {
        if (level != null && level.isClientSide()) {
            return clientSeat.isPresent() ? clientFrameRings : scan.rings();
        }
        return frame().scan().rings();
    }

    /** The Core's rank (requirements §4.6); 1 for a block that is not a Core. */
    public int rank() {
        return getBlockState().getBlock() instanceof CircleCoreBlock core ? core.rank() : 1;
    }

    /** How many rune slots this Core has. */
    public int slots() {
        return getBlockState().getBlock() instanceof CircleCoreBlock core ? core.slots() : 3;
    }

    /** How many rings this Core reads. */
    public int maxRings() {
        return getBlockState().getBlock() instanceof CircleCoreBlock core ? core.maxRings() : 3;
    }

    /** The circle's front: away from the face the Core is drawn on. */
    public Direction front() {
        return ChalkPatternBlock.front(getBlockState());
    }

    public CircleScan scan() {
        return scan;
    }

    public int instability() {
        return instability;
    }

    /** The threshold to show: the server's, even on the client. */
    public int displayThreshold() {
        return level != null && level.isClientSide() ? clientThreshold : settings.instabilityThreshold();
    }

    // Runes and Essentia

    /** Aspect ids of the runes, slot 1 first. */
    public List<Identifier> runes() {
        return runes;
    }

    /** Puts a rune in the first empty slot. False when all are full. */
    public boolean insert(Identifier aspect) {
        if (runes.size() >= slots()) {
            return false;
        }
        List<Identifier> updated = new ArrayList<>(runes);
        updated.add(aspect);
        setRunes(updated);
        return true;
    }

    /** Takes the rune out of the last filled slot. */
    public Optional<Identifier> removeLast() {
        if (runes.isEmpty()) {
            return Optional.empty();
        }
        Identifier last = runes.getLast();
        setRunes(runes.subList(0, runes.size() - 1));
        return Optional.of(last);
    }

    private void setRunes(List<Identifier> updated) {
        runes = List.copyOf(updated);
        setChanged();
        if (level != null && !level.isClientSide()) {
            stopIfBroken();
            updateIndex();
            sync();
        }
    }

    /** Keeps this Core listed under its combination while its circle works, so others can find it. */
    private void updateIndex() {
        if (level instanceof ServerLevel server) {
            CircleIndex index = CircleIndex.of(server);
            Optional<CircleCombination> combination = combination().filter(c -> circle().filter(this::runsHere).isPresent());
            if (combination.isPresent()) {
                index.put(worldPosition, combination.get());
            } else {
                index.remove(worldPosition);
            }
        }
    }

    /**
     * For pipes and other mods: only the runes' aspects go in, each up to the capacity, and
     * nothing comes out (requirements §8.2).
     */
    private final EssentiaContainer container = new EssentiaContainer() {
        @Override
        public AspectList contents() {
            return essentia;
        }

        @Override
        public int space(AspectList contents, Aspect aspect) {
            return acceptedAspects().contains(aspect) ? Math.max(0, capacity() - contents.amount(aspect)) : 0;
        }

        @Override
        public boolean canExtract(Aspect aspect) {
            return false;
        }

        @Override
        public void update(AspectList contents) {
            setEssentia(contents);
        }
    };

    public EssentiaContainer container() {
        return container;
    }

    public AspectList essentia() {
        return essentia;
    }

    public void setEssentia(AspectList updated) {
        if (updated.equals(essentia)) {
            return;
        }
        essentia = updated;
        setChanged();
        if (level != null && !level.isClientSide()) {
            sync();
        }
    }

    /** The aspects the runes let in; only these can be poured into the Core. */
    public Set<Aspect> acceptedAspects() {
        return runes.stream().flatMap(id -> ThaumoryApi.aspects().get(id).stream()).collect(Collectors.toUnmodifiableSet());
    }

    /** How much of each aspect the Core holds: the server's, even on the client. */
    public int displayCapacity() {
        return level != null && level.isClientSide() ? clientCapacity : capacity();
    }

    public static int capacity() {
        return settings.essentiaCapacity();
    }

    // The circle

    private record Circle(CircleDefinitions.Definition definition, Optional<Aspect> parameter, CircleSettings.Multipliers multipliers, Frame frame) {
        /** The aspects whose colours mark what the effect reaches: slots 1 and 2. */
        List<Aspect> colours() {
            return List.of(definition.first(), definition.second());
        }
    }

    /** The combination the runes and chalk make now, if the datapacks define it. */
    private Optional<Circle> circle() {
        Frame frame = frame();
        if (frame.scan().rings() == 0 || runes.size() < 2) {
            return Optional.empty();
        }
        Optional<Aspect> first = ThaumoryApi.aspects().get(runes.get(0));
        Optional<Aspect> second = ThaumoryApi.aspects().get(runes.get(1));
        Optional<Aspect> parameter = runes.size() > 2 ? ThaumoryApi.aspects().get(runes.get(2)) : Optional.empty();
        Optional<Aspect> fourth = runes.size() > 3 ? ThaumoryApi.aspects().get(runes.get(3)) : Optional.empty();
        if (first.isEmpty() || second.isEmpty() || (runes.size() > 2 && parameter.isEmpty()) || (runes.size() > 3 && fourth.isEmpty())) {
            return Optional.empty();
        }
        return CircleDefinitionReloadListener.definitions().find(first.get(), second.get(), parameter, fourth)
                .map(definition -> new Circle(definition, parameter, settings.multipliers(frame.scan().nodes(), frame.rank()), frame));
    }

    /** Whether this Core is of high enough rank to run {@code circle}. */
    private boolean runsHere(Circle circle) {
        return circle.definition().rank() <= rank();
    }

    /**
     * Whether the runes and chalk make a defined combination that needs a higher rank than this
     * Core's; what was last received on the client.
     */
    public boolean lowRank() {
        if (level != null && level.isClientSide()) {
            return clientLowRank;
        }
        return circle().filter(circle -> !runsHere(circle)).isPresent();
    }

    /** How the circle the runes and chalk make now is scaled, if it is defined. Server only. */
    public Optional<CircleSettings.Multipliers> multipliers() {
        return circle().map(Circle::multipliers);
    }

    /** What the circle would take now, if it is defined; what was last received on the client. */
    public Optional<Upkeep> upkeep() {
        if (level != null && level.isClientSide()) {
            return clientUpkeep;
        }
        return circle().map(circle -> {
            CircleDefinitions.Definition definition = circle.definition();
            double cost = circle.multipliers().cost();
            return definition.mode() == CircleMode.TRIGGERED
                    ? new Upkeep(CircleMode.TRIGGERED, CircleUpkeep.triggeredCost(definition.cost(), cost), 0)
                    : new Upkeep(CircleMode.SUSTAINED, 1, CircleUpkeep.sustainedInterval(definition.interval(), cost));
        });
    }

    /** The effect the runes and chalk make now, if defined; what was last received on the client. */
    /**
     * Whether a wand's click with an item on the pedestal infuses it: true unless the circle works and
     * its effect cannot be burnt into anything (a charging circle, say), which then starts and stops as usual.
     */
    public boolean infusesPedestalItem() {
        return effectId().map(ThaumoryApi.infusionEffects()::contains).orElse(true);
    }

    public Optional<Identifier> effectId() {
        if (level != null && level.isClientSide()) {
            return clientEffect;
        }
        return circle().map(circle -> circle.definition().effect());
    }

    /** The runes as the book records circles, once there are at least two. */
    public Optional<CircleCombination> combination() {
        if (runes.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(new CircleCombination(runes.get(0), runes.get(1), runes.size() > 2 ? Optional.of(runes.get(2)) : Optional.empty(),
                runes.size() > 3 ? Optional.of(runes.get(3)) : Optional.empty()));
    }

    /** The one who started or triggered the circle has now seen it work; the first time, they are told what it is. */
    private void recordSuccess(Optional<Entity> activator, Identifier effect) {
        if (!(activator.orElse(null) instanceof ServerPlayer player) || combination().isEmpty()) {
            return;
        }
        CircleCombination combination = combination().get();
        boolean known = Thaumory.knowledge().get(player).circle(combination).filter(PlayerKnowledge.CircleOutcome.SUCCESS::equals).isPresent();
        Thaumory.knowledge().update(player, k -> k.withCircle(combination, PlayerKnowledge.CircleOutcome.SUCCESS));
        if (!known) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ThaumorySounds.DISCOVERY.get(), SoundSource.PLAYERS,
                    0.8f, 1.0f);
            player.sendSystemMessage(Component.translatable("message.thaumory.circle.discovered",
                    ThaumoryText.withEffect(Component.translatable(effect.toLanguageKey("circle_effect")).withColor(0xCC99FF), TextEffect.STREAK)));
        }
    }

    /**
     * Tried with runes that make no defined combination: Flux leaks out, and the one who tried
     * learns it fails. False when there are not yet two runes, which is not a combination at all.
     */
    private boolean misfire(ServerLevel server, Optional<Entity> activator) {
        Optional<CircleCombination> combination = combination();
        if (combination.isEmpty()) {
            return false;
        }
        if (activator.orElse(null) instanceof ServerPlayer player) {
            Thaumory.knowledge().update(player, k -> k.withCircle(combination.get(), PlayerKnowledge.CircleOutcome.FAILURE));
        }
        releaseFlux(server, settings.undefinedFlux());
        return true;
    }

    /** Each payment of a circle over its instability threshold may leak Flux; a sub-circle goes by its parent's instability. */
    private void rollInstability(ServerLevel server, Circle circle) {
        releaseFlux(server, settings.instabilityFlux(circle.frame().instability(), server.getRandom().nextDouble()));
    }

    private FluxStage fluxStage(ServerLevel server) {
        return Thaumory.flux().stage(server, ChunkPos.containing(worldPosition));
    }

    /**
     * At overload, a payment may misfire: the effect does not happen, the Essentia paid is lost,
     * Flux leaks out and a blast that breaks nothing hurts what is near and pollutes the ground.
     */
    private boolean misfire(ServerLevel server) {
        FluxSettings.Effects effects = Thaumory.flux().settings().effects();
        double chance = effects.misfireChance(fluxStage(server));
        if (chance <= 0 || server.getRandom().nextDouble() >= chance) {
            return false;
        }
        releaseFlux(server, settings.undefinedFlux());
        double radius = effects.explosionRadius();
        Vec3 center = Vec3.atCenterOf(worldPosition);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.5, center.z, 60, radius / 2, 0.5, radius / 2, 0.1);
        server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(radius))) {
            Vec3 away = entity.position().subtract(center);
            if (away.length() > radius) {
                continue;
            }
            entity.hurtServer(server, server.damageSources().magic(), effects.explosionDamage());
            Vec3 push = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize();
            entity.push(push.x, 0.4, push.z);
            entity.needsSync = true;
        }
        FluxWorldEffects.polluteAround(server, worldPosition, radius);
        return true;
    }

    private void releaseFlux(ServerLevel server, double amount) {
        if (amount <= 0) {
            return;
        }
        ThaumoryApi.flux().add(server, ChunkPos.containing(worldPosition), amount);
        server.sendParticles(ParticleTypes.WITCH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5,
                12, 0.6, 0.2, 0.6, 0);
        server.playSound(null, worldPosition, ThaumorySounds.CIRCLE_FLUX.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    public boolean isRunning() {
        return running.isPresent();
    }

    /** Client only: when a triggered circle last went off, in game time. */
    public long lastFlash() {
        return lastFlash;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == FLASH_EVENT) {
            if (level != null) {
                lastFlash = level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    public StartResult start(Optional<Entity> activator) {
        if (!(level instanceof ServerLevel server)) {
            return StartResult.UNDEFINED;
        }
        if (running.isPresent()) {
            return StartResult.ALREADY_RUNNING;
        }
        if (frame().scan().rings() == 0) {
            return StartResult.NO_RINGS;
        }
        Optional<Circle> circle = circle();
        if (circle.isEmpty()) {
            return misfire(server, activator) ? StartResult.MISFIRED : StartResult.UNDEFINED;
        }
        if (!runsHere(circle.get())) {
            return StartResult.LOW_RANK;
        }
        CircleDefinitions.Definition definition = circle.get().definition();
        if (definition.mode() != CircleMode.SUSTAINED) {
            return StartResult.TRIGGERED_ONLY;
        }
        Optional<AspectList> paid = CircleUpkeep.paySustained(essentia, definition.first(), definition.second());
        if (paid.isEmpty()) {
            return StartResult.NO_ESSENTIA;
        }
        setEssentia(paid.get());
        if (misfire(server)) {
            return StartResult.OVERLOADED;
        }
        rollInstability(server, circle.get());
        running = Optional.of(definition.id());
        runningEffect = definition.effect();
        runningRunes = runes;
        effectData = new CompoundTag();
        nextPayment = server.getGameTime() + CircleUpkeep.sustainedInterval(definition.interval(), circle.get().multipliers().cost());
        effect(runningEffect).ifPresent(effect -> effect.apply(context(server, circle.get(), Optional.empty())));
        recordSuccess(activator, runningEffect);
        setChanged();
        sync();
        return StartResult.STARTED;
    }

    /**
     * Stops a sustained circle, or a triggered one still at work, letting its effect clean up. False
     * if it was doing neither.
     */
    public boolean stop() {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (running.isEmpty()) {
            return stopWork(server);
        }
        Identifier effectId = runningEffect;
        Optional<Circle> circle = circle();
        CircleContext context = new Context(server, circle, Optional.empty());
        effect(effectId).ifPresent(effect -> effect.stop(context));
        running = Optional.empty();
        runningEffect = null;
        runningRunes = List.of();
        effectData = new CompoundTag();
        setChanged();
        sync();
        return true;
    }

    public TriggerResult trigger(Optional<Entity> activator) {
        if (!(level instanceof ServerLevel server)) {
            return TriggerResult.UNDEFINED;
        }
        if (frame().scan().rings() == 0) {
            return TriggerResult.NO_RINGS;
        }
        Optional<Circle> circle = circle();
        if (circle.isEmpty()) {
            return misfire(server, activator) ? TriggerResult.MISFIRED : TriggerResult.UNDEFINED;
        }
        if (!runsHere(circle.get())) {
            return TriggerResult.LOW_RANK;
        }
        CircleDefinitions.Definition definition = circle.get().definition();
        if (definition.mode() != CircleMode.TRIGGERED) {
            return TriggerResult.SUSTAINED_ONLY;
        }
        Optional<CircleEffect> effect = effect(definition.effect());
        CircleContext context = context(server, circle.get(), activator);
        if (effect.isPresent() && definition.effect().equals(workingEffect) && effect.get().working(context)) {
            stopWork(server);
            return TriggerResult.STOPPED;
        }
        if (effect.isEmpty() || !effect.get().canApply(context)) {
            return TriggerResult.NO_TARGET;
        }
        int cost = CircleUpkeep.triggeredCost(definition.cost(), circle.get().multipliers().cost());
        Optional<AspectList> paid = CircleUpkeep.payTriggered(essentia, definition.first(), definition.second(), circle.get().parameter(), cost);
        if (paid.isEmpty()) {
            return TriggerResult.NO_ESSENTIA;
        }
        setEssentia(paid.get());
        if (misfire(server)) {
            return TriggerResult.OVERLOADED;
        }
        rollInstability(server, circle.get());
        effect.get().apply(context);
        server.blockEvent(worldPosition, getBlockState().getBlock(), FLASH_EVENT, 0);
        workingEffect = effect.get().working(context) ? definition.effect() : null;
        setChanged();
        recordSuccess(activator, definition.effect());
        return TriggerResult.TRIGGERED;
    }

    public boolean hasPedestal() {
        return getBlockState().getValue(CircleCoreBlock.PEDESTAL);
    }

    /** The item on the built-in pedestal; empty without one. */
    public ItemStack pedestalItem() {
        return pedestalItem;
    }

    public void setPedestalItem(ItemStack stack) {
        pedestalItem = stack;
        setChanged();
        sync();
    }

    public ItemStack takePedestalItem() {
        ItemStack taken = pedestalItem;
        setPedestalItem(ItemStack.EMPTY);
        return taken;
    }

    /** Takes the pedestal out again, with whatever is on it; both go to {@code player}. */
    public void removePedestal(Player player) {
        if (level == null || !hasPedestal()) {
            return;
        }
        if (!pedestalItem.isEmpty()) {
            player.getInventory().placeItemBackInInventory(takePedestalItem(), Prediction.SERVER_ONLY);
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(ThaumoryItems.PEDESTAL.get()), Prediction.SERVER_ONLY);
        level.setBlock(worldPosition, getBlockState().setValue(CircleCoreBlock.PEDESTAL, false), Block.UPDATE_ALL);
    }

    /**
     * Burns this circle's effect into the item on the pedestal (requirements §10.1). It takes much
     * Essentia and may fail, losing it and releasing Flux; the item never breaks. A sustained
     * circle keeps running.
     */
    public InfuseOutcome infuse(Optional<Entity> activator) {
        if (!(level instanceof ServerLevel server)) {
            return InfuseOutcome.of(InfuseResult.UNDEFINED);
        }
        if (!hasPedestal() || pedestalItem.isEmpty()) {
            return InfuseOutcome.of(InfuseResult.NO_ITEM);
        }
        if (frame().scan().rings() == 0) {
            return InfuseOutcome.of(InfuseResult.NO_RINGS);
        }
        Optional<Circle> circle = circle();
        if (circle.isEmpty()) {
            return InfuseOutcome.of(misfire(server, activator) ? InfuseResult.MISFIRED : InfuseResult.UNDEFINED);
        }
        if (!runsHere(circle.get())) {
            return InfuseOutcome.of(InfuseResult.LOW_RANK);
        }
        ItemStack stack = pedestalItem;
        Optional<InfusionEffect> effect = ThaumoryApi.infusionEffects().get(circle.get().definition().effect())
                .filter(e -> e.castable() || !stack.is(ThaumoryItems.BLANK_SCROLL.get()));
        if (effect.isEmpty()) {
            return InfuseOutcome.of(InfuseResult.NOT_INFUSABLE);
        }
        int capacity = InfusionCapacities.of(stack.getItem());
        if (capacity == 0) {
            return InfuseOutcome.of(InfuseResult.NO_CAPACITY);
        }
        CircleDefinitions.Definition definition = circle.get().definition();
        CircleSettings.Multipliers multipliers = circle.get().multipliers();
        InfusionSettings infusion = settings.infusion();
        Infusion burnt = new Infusion(definition.effect(), InfusionRules.level(multipliers.strength(), infusion),
                circle.get().parameter().map(Aspect::id), definition.capacity(),
                effect.get().active() ? InfusionRules.useCost(definition.itemCost(), multipliers.cost(), definition.first(), definition.second())
                        : Map.of());
        Infusions infusions = stack.getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY);
        if (!infusions.fits(burnt, capacity)) {
            return new InfuseOutcome(InfuseResult.NO_ROOM, Optional.of(burnt), infusions.usedBesides(burnt.effect()), capacity);
        }
        if (!infusions.allowsActive(burnt)) {
            return InfuseOutcome.of(InfuseResult.ACTIVE_TAKEN);
        }
        AspectList cost = InfusionRules.cost(definition.infusionCost(), multipliers.cost(), definition.first(), definition.second(),
                circle.get().parameter());
        if (!essentia.containsAll(cost)) {
            return InfuseOutcome.of(InfuseResult.NO_ESSENTIA);
        }
        setEssentia(essentia.minus(cost));
        if (server.getRandom().nextDouble() < InfusionRules.failureChance(circle.get().frame().instability(), settings.instabilityThreshold(), infusion)) {
            releaseFlux(server, InfusionRules.failureFlux(cost, infusion));
            return InfuseOutcome.of(InfuseResult.FAILED);
        }
        ItemStack infused = ThaumoryItems.infusedForm(stack);
        infused.set(ThaumoryComponents.INFUSIONS.get(), infusions.with(burnt));
        setPedestalItem(infused);
        server.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                40, 0.3, 0.3, 0.3, 0.6);
        recordSuccess(activator, definition.effect());
        return new InfuseOutcome(InfuseResult.INFUSED, Optional.of(burnt), infusions.usedBesides(burnt.effect()) + burnt.capacity(), capacity);
    }

    private void runSustained(ServerLevel server, long time) {
        Optional<Circle> circle = circle();
        if (circle.isEmpty() || !stillTheSame(circle.get())) {
            stop();
            return;
        }
        CircleDefinitions.Definition definition = circle.get().definition();
        if (time >= nextPayment) {
            Optional<AspectList> paid = CircleUpkeep.paySustained(essentia, definition.first(), definition.second());
            if (paid.isEmpty()) {
                stop();
                return;
            }
            setEssentia(paid.get());
            if (misfire(server)) {
                stop();
                return;
            }
            rollInstability(server, circle.get());
            nextPayment = time + CircleUpkeep.sustainedInterval(definition.interval(), circle.get().multipliers().cost());
            setChanged();
        }
        Optional<CircleEffect> effect = effect(runningEffect);
        if (effect.isPresent() && Math.floorMod(time + worldPosition.hashCode(), Math.max(1, effect.get().period())) == 0) {
            effect.get().apply(context(server, circle.get(), Optional.empty()));
            setChanged();
        }
    }

    /**
     * Stops the work a triggered effect's activation left going. Its data stays, so the next
     * activation can carry on where this one left off. False if there was none.
     */
    private boolean stopWork(ServerLevel server) {
        if (workingEffect == null) {
            return false;
        }
        Optional<Circle> circle = circle();
        CircleContext context = new Context(server, circle, Optional.empty());
        effect(workingEffect).ifPresent(effect -> effect.stop(context));
        workingEffect = null;
        setChanged();
        return true;
    }

    /**
     * Carries on a triggered effect's work every period while the circle that started it still
     * stands; it is given up if the circle breaks or changes.
     */
    private void continueWork(ServerLevel server, long time) {
        Optional<Circle> circle = circle();
        Optional<CircleEffect> effect = effect(workingEffect);
        if (circle.isEmpty() || effect.isEmpty() || circle.get().definition().mode() != CircleMode.TRIGGERED
                || !circle.get().definition().effect().equals(workingEffect)) {
            workingEffect = null;
            setChanged();
            return;
        }
        if (Math.floorMod(time + worldPosition.hashCode(), Math.max(1, effect.get().period())) != 0) {
            return;
        }
        CircleContext context = context(server, circle.get(), Optional.empty());
        if (effect.get().working(context)) {
            effect.get().work(context);
        }
        if (!effect.get().working(context)) {
            workingEffect = null;
        }
        setChanged();
    }

    /** A running circle stops when its rings go, its runes change, or the datapacks no longer define it the same. */
    private void stopIfBroken() {
        if (running.isPresent() && circle().filter(this::stillTheSame).isEmpty()) {
            stop();
        }
    }

    private boolean stillTheSame(Circle circle) {
        return runes.equals(runningRunes) && running.filter(circle.definition().id()::equals).isPresent();
    }

    private static Optional<CircleEffect> effect(Identifier id) {
        return id == null ? Optional.empty() : ThaumoryApi.circleEffects().get(id);
    }

    private CircleContext context(ServerLevel server, Circle circle, Optional<Entity> activator) {
        return new Context(server, Optional.of(circle), activator);
    }

    /** What an effect sees of this Core while it works, or while it stops with its circle gone ({@code circle} empty). */
    private final class Context implements CircleContext {
        private final ServerLevel level;
        private final Optional<Circle> circle;
        private final Optional<Entity> activator;

        Context(ServerLevel level, Optional<Circle> circle, Optional<Entity> activator) {
            this.level = level;
            this.circle = circle;
            this.activator = activator;
        }

        @Override
        public ServerLevel level() {
            return level;
        }

        @Override
        public BlockPos core() {
            return worldPosition;
        }

        @Override
        public BlockPos centre() {
            return circle.map(c -> c.frame().centre()).orElse(worldPosition);
        }

        @Override
        public Optional<Aspect> parameter() {
            return circle.flatMap(Circle::parameter);
        }

        @Override
        public double strength() {
            return circle.map(c -> c.multipliers().strength()).orElse(1.0);
        }

        @Override
        public double radius() {
            return circle.map(c -> settings.radius(c.frame().scan().rings(), c.multipliers())).orElse(0.0);
        }

        @Override
        public Optional<Entity> activator() {
            return activator;
        }

        @Override
        public CompoundTag data() {
            return effectData;
        }

        @Override
        public double setting(String key, double fallback) {
            return circle.map(c -> c.definition().settings().get(key)).orElse(null) instanceof Double value ? value : fallback;
        }

        @Override
        public int store(Aspect aspect, int amount) {
            if (amount <= 0 || !acceptedAspects().contains(aspect)) {
                return 0;
            }
            int stored = Math.min(amount, Math.max(0, capacity() - essentia.amount(aspect)));
            if (stored > 0) {
                setEssentia(essentia.plus(AspectList.of(aspect, stored)));
            }
            return stored;
        }

        @Override
        public void affected(BlockPos pos) {
            ThaumoryParticles.motes(level, pos, colours());
            giveOffWorkFlux(level, setting(WORK_FLUX, 0));
        }

        @Override
        public void affected(Entity entity) {
            ThaumoryParticles.motes(level, entity, colours());
            giveOffWorkFlux(level, setting(WORK_FLUX, 0));
        }

        @Override
        public boolean pay(int amount) {
            if (circle.isEmpty() || amount <= 0) {
                return circle.isPresent();
            }
            CircleDefinitions.Definition definition = circle.get().definition();
            Optional<AspectList> paid = CircleUpkeep.payWork(essentia, definition.first(), definition.second(), amount,
                    circle.get().multipliers().cost());
            paid.ifPresent(CircleCoreBlockEntity.this::setEssentia);
            return paid.isPresent();
        }

        private List<Aspect> colours() {
            return circle.map(Circle::colours).orElse(List.of());
        }
    }

    /** The Flux a working circle gives off (requirements §4.5), held back until it comes to a whole unit. */
    private void giveOffWorkFlux(ServerLevel server, double gain) {
        if (gain <= 0) {
            return;
        }
        ChunkPos chunk = ChunkPos.containing(worldPosition);
        WorkFlux.Step step = WorkFlux.add(workFlux, gain, ThaumoryApi.flux().get(server, chunk));
        if (step.released() > 0) {
            ThaumoryApi.flux().add(server, chunk, step.released());
        }
        workFlux = step.held();
        setChanged();
    }

    // Saving and syncing

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            stop();
            CircleIndex.of(server).remove(pos);
            for (Identifier aspect : runes) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RuneItem.of(aspect));
            }
            if (state.getValue(CircleCoreBlock.PEDESTAL)) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ThaumoryItems.PEDESTAL.get()));
            }
            if (!pedestalItem.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, pedestalItem);
            }
            if (essentia.total() > 0) {
                ThaumoryApi.flux().add(server, ChunkPos.containing(pos), essentia.total());
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!pedestalItem.isEmpty()) {
            output.store("pedestal_item", ItemStack.CODEC, pedestalItem);
        }
        if (!runes.isEmpty()) {
            output.store("runes", RUNES_CODEC, runes);
        }
        if (!essentia.isEmpty()) {
            output.store("essentia", ESSENTIA_CODEC, essentia);
        }
        running.ifPresent(id -> {
            output.store("running", Identifier.CODEC, id);
            output.store("running_effect", Identifier.CODEC, runningEffect);
            output.store("running_runes", RUNES_CODEC, runningRunes);
            output.putLong("next_payment", nextPayment);
        });
        // A triggered circle keeps its data between activations too (how deep the mining circle got, say).
        if (!effectData.isEmpty()) {
            output.store("effect_data", CompoundTag.CODEC, effectData);
        }
        if (workingEffect != null) {
            output.store("working_effect", Identifier.CODEC, workingEffect);
        }
        if (workFlux > 0) {
            output.putDouble("work_flux", workFlux);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        runes = input.read("runes", RUNES_CODEC).orElse(List.of());
        essentia = input.read("essentia", ESSENTIA_CODEC).orElse(AspectList.empty());
        pedestalItem = input.read("pedestal_item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        running = input.read("running", Identifier.CODEC);
        runningEffect = input.read("running_effect", Identifier.CODEC).orElse(null);
        runningRunes = input.read("running_runes", RUNES_CODEC).orElse(runes);
        nextPayment = input.getLongOr("next_payment", 0);
        effectData = input.read("effect_data", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        workingEffect = input.read("working_effect", Identifier.CODEC).orElse(null);
        workFlux = input.getDoubleOr("work_flux", 0);
        // Sent only to clients.
        input.read("scan", SCAN_CODEC).ifPresent(received -> scan = received);
        instability = input.getIntOr("instability", instability);
        clientThreshold = input.getIntOr("threshold", clientThreshold);
        clientCapacity = input.getIntOr("capacity", clientCapacity);
        clientUpkeep = input.read("upkeep", Upkeep.CODEC);
        clientEffect = input.read("effect", Identifier.CODEC);
        clientLowRank = input.getBooleanOr("low_rank", false);
        clientChildren = input.getIntOr("children", 0);
        clientSeat = input.getString("seat").map(name -> CircleChildren.Seat.valueOf(name));
        clientSeatSide = input.getString("seat_side").map(name -> CircleSide.valueOf(name));
        clientFrameRings = input.getIntOr("frame_rings", 0);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        // Effects keep their own state; clients have no use for it.
        tag.remove("effect_data");
        SCAN_CODEC.encodeStart(NbtOps.INSTANCE, scan).ifSuccess(encoded -> tag.put("scan", encoded));
        tag.putInt("instability", instability);
        tag.putInt("threshold", settings.instabilityThreshold());
        tag.putInt("capacity", capacity());
        upkeep().flatMap(u -> Upkeep.CODEC.encodeStart(NbtOps.INSTANCE, u).result()).ifPresent(encoded -> tag.put("upkeep", encoded));
        effectId().ifPresent(effect -> tag.putString("effect", effect.toString()));
        if (lowRank()) {
            tag.putBoolean("low_rank", true);
        }
        if (children() > 0) {
            tag.putInt("children", children());
        }
        seat().ifPresent(seat -> {
            tag.putString("seat", seat.name());
            seatSide().ifPresent(side -> tag.putString("seat_side", side.name()));
            tag.putInt("frame_rings", frameRings());
        });
        return tag;
    }
}
