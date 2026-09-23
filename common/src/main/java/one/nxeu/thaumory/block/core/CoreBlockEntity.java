package one.nxeu.thaumory.block.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.circle.CircleDefinitions;
import one.nxeu.thaumory.circle.CircleMode;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.circle.CircleSide;
import one.nxeu.thaumory.circle.CircleUpkeep;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/**
 * A magic circle's Core: up to three runes, kept by aspect id in slot order, the Essentia poured
 * in for them, and the chalk around it as last scanned. The scan is not saved; it runs again every
 * {@code scan_interval} ticks and goes to clients when it changes, for the loupe.
 *
 * <p>A sustained circle, once started, pays its upkeep on its interval and works once a second
 * until it is stopped, runs dry, or its circle breaks.
 */
public final class CoreBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;
    private static final int EFFECT_INTERVAL = 20;
    private static final Codec<List<Identifier>> RUNES_CODEC = Identifier.CODEC.listOf(0, SLOTS);
    private static final Codec<AspectList> ESSENTIA_CODEC = AspectCodecs.aspectList(ThaumoryApi.aspects());
    private static final CircleScan UNSCANNED = new CircleScan(0, List.of(), List.of());
    /** The scan as the loupe needs it on the client; sent with block updates, never saved. */
    private static final Codec<CircleScan> SCAN_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("rings").forGetter(CircleScan::rings),
            RecordCodecBuilder.<CircleScan.Node>create(n -> n.group(
                    Codec.INT.fieldOf("ring").forGetter(CircleScan.Node::ring),
                    Codec.STRING.xmap(CircleSide::valueOf, CircleSide::name).fieldOf("side").forGetter(CircleScan.Node::side),
                    Identifier.CODEC.fieldOf("pattern").forGetter(CircleScan.Node::pattern)
            ).apply(n, CircleScan.Node::new)).listOf().fieldOf("nodes").forGetter(CircleScan::nodes),
            RecordCodecBuilder.<CircleScan.Offset>create(o -> o.group(
                    Codec.INT.fieldOf("dx").forGetter(CircleScan.Offset::dx),
                    Codec.INT.fieldOf("dz").forGetter(CircleScan.Offset::dz)
            ).apply(o, CircleScan.Offset::new)).listOf().fieldOf("ignored").forGetter(CircleScan::ignoredModifiers)
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

    public enum StartResult { STARTED, ALREADY_RUNNING, NO_RINGS, UNDEFINED, TRIGGERED_ONLY, NO_ESSENTIA }

    public enum TriggerResult { TRIGGERED, NO_RINGS, UNDEFINED, SUSTAINED_ONLY, NO_ESSENTIA }

    private List<Identifier> runes = List.of();
    private AspectList essentia = AspectList.empty();
    private CircleScan scan = UNSCANNED;
    private int instability;
    /** The server's view, as last received. Only meaningful on the client. */
    private int clientThreshold = CircleSettings.DEFAULT.instabilityThreshold();
    private int clientCapacity = CircleSettings.DEFAULT.essentiaCapacity();
    private Optional<Upkeep> clientUpkeep = Optional.empty();
    private Optional<Identifier> clientEffect = Optional.empty();

    /** The running sustained circle's definition id, or empty when it is not running. */
    private Optional<Identifier> running = Optional.empty();
    private Identifier runningEffect;
    /** The runes the running circle started with; any change stops it. */
    private List<Identifier> runningRunes = List.of();
    private long nextPayment;
    private CompoundTag effectData = new CompoundTag();

    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.CORE_ENTITY.get(), pos, state);
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
    public static void serverTick(Level level, BlockPos pos, BlockState state, CoreBlockEntity core) {
        long time = level.getGameTime();
        if (Math.floorMod(time + pos.hashCode(), settings.scanInterval()) == 0) {
            core.rescan();
        }
        if (core.running.isPresent() && level instanceof ServerLevel server) {
            core.runSustained(server, time);
        }
    }

    // Scanning

    public void rescan() {
        if (level == null) {
            return;
        }
        Identifier line = BuiltInRegistries.BLOCK.getKey(ThaumoryBlocks.CHALK_LINE.get());
        CircleScan next = CircleScan.scan((dx, dz) -> {
            BlockState state = level.getBlockState(worldPosition.offset(dx, 0, dz));
            return state.is(ChalkPatternBlock.PATTERNS) ? Optional.of(BuiltInRegistries.BLOCK.getKey(state.getBlock())) : Optional.empty();
        }, line);
        CircleScan previousScan = scan;
        int previousInstability = instability;
        scan = next;
        instability = settings.instability(scan.nodes());
        if (!level.isClientSide()) {
            stopIfBroken();
            if (!scan.equals(previousScan) || instability != previousInstability) {
                sync();
            }
        }
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
        if (runes.size() >= SLOTS) {
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
            sync();
        }
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

    private record Circle(CircleDefinitions.Definition definition, Optional<Aspect> parameter, CircleSettings.Multipliers multipliers) {}

    /** The combination the runes and chalk make now, if the datapacks define it. */
    private Optional<Circle> circle() {
        if (scan.rings() == 0 || runes.size() < 2) {
            return Optional.empty();
        }
        Optional<Aspect> first = ThaumoryApi.aspects().get(runes.get(0));
        Optional<Aspect> second = ThaumoryApi.aspects().get(runes.get(1));
        Optional<Aspect> parameter = runes.size() > 2 ? ThaumoryApi.aspects().get(runes.get(2)) : Optional.empty();
        if (first.isEmpty() || second.isEmpty() || (runes.size() > 2 && parameter.isEmpty())) {
            return Optional.empty();
        }
        return CircleDefinitionReloadListener.definitions().find(first.get(), second.get(), parameter)
                .map(definition -> new Circle(definition, parameter, settings.multipliers(scan.nodes())));
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
        return Optional.of(new CircleCombination(runes.get(0), runes.get(1), runes.size() > 2 ? Optional.of(runes.get(2)) : Optional.empty()));
    }

    /** The one who started or triggered the circle has now seen it work. */
    private void recordSuccess(Optional<Entity> activator) {
        if (activator.orElse(null) instanceof ServerPlayer player) {
            combination().ifPresent(combination ->
                    Thaumory.knowledge().update(player, k -> k.withCircle(combination, PlayerKnowledge.CircleOutcome.SUCCESS)));
        }
    }

    public boolean isRunning() {
        return running.isPresent();
    }

    public StartResult start(Optional<Entity> activator) {
        if (!(level instanceof ServerLevel server)) {
            return StartResult.UNDEFINED;
        }
        if (running.isPresent()) {
            return StartResult.ALREADY_RUNNING;
        }
        if (scan.rings() == 0) {
            return StartResult.NO_RINGS;
        }
        Optional<Circle> circle = circle();
        if (circle.isEmpty()) {
            return StartResult.UNDEFINED;
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
        running = Optional.of(definition.id());
        runningEffect = definition.effect();
        runningRunes = runes;
        effectData = new CompoundTag();
        nextPayment = server.getGameTime() + CircleUpkeep.sustainedInterval(definition.interval(), circle.get().multipliers().cost());
        effect(runningEffect).ifPresent(effect -> effect.apply(context(server, circle.get(), Optional.empty())));
        recordSuccess(activator);
        setChanged();
        sync();
        return StartResult.STARTED;
    }

    /** Stops a sustained circle, letting its effect clean up. False if it was not running. */
    public boolean stop() {
        if (running.isEmpty() || !(level instanceof ServerLevel server)) {
            return false;
        }
        Identifier effectId = runningEffect;
        Optional<Circle> circle = circle();
        CircleContext context = new Context(server, worldPosition, circle.flatMap(Circle::parameter),
                circle.map(c -> c.multipliers().strength()).orElse(1.0),
                circle.map(c -> settings.radius(scan.rings(), c.multipliers())).orElse(0.0), Optional.empty(), effectData);
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
        if (scan.rings() == 0) {
            return TriggerResult.NO_RINGS;
        }
        Optional<Circle> circle = circle();
        if (circle.isEmpty()) {
            return TriggerResult.UNDEFINED;
        }
        CircleDefinitions.Definition definition = circle.get().definition();
        if (definition.mode() != CircleMode.TRIGGERED) {
            return TriggerResult.SUSTAINED_ONLY;
        }
        int cost = CircleUpkeep.triggeredCost(definition.cost(), circle.get().multipliers().cost());
        Optional<AspectList> paid = CircleUpkeep.payTriggered(essentia, definition.first(), definition.second(), circle.get().parameter(), cost);
        if (paid.isEmpty()) {
            return TriggerResult.NO_ESSENTIA;
        }
        setEssentia(paid.get());
        effect(definition.effect()).ifPresent(effect -> effect.apply(context(server, circle.get(), activator)));
        recordSuccess(activator);
        return TriggerResult.TRIGGERED;
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
            nextPayment = time + CircleUpkeep.sustainedInterval(definition.interval(), circle.get().multipliers().cost());
            setChanged();
        }
        if (Math.floorMod(time + worldPosition.hashCode(), EFFECT_INTERVAL) == 0) {
            effect(runningEffect).ifPresent(effect -> effect.apply(context(server, circle.get(), Optional.empty())));
            setChanged();
        }
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
        return new Context(server, worldPosition, circle.parameter(), circle.multipliers().strength(),
                settings.radius(scan.rings(), circle.multipliers()), activator, effectData);
    }

    private record Context(ServerLevel level, BlockPos core, Optional<Aspect> parameter, double strength, double radius,
            Optional<Entity> activator, CompoundTag data) implements CircleContext {}

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
            for (Identifier aspect : runes) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RuneItem.of(aspect));
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
            output.store("effect_data", CompoundTag.CODEC, effectData);
        });
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        runes = input.read("runes", RUNES_CODEC).orElse(List.of());
        essentia = input.read("essentia", ESSENTIA_CODEC).orElse(AspectList.empty());
        running = input.read("running", Identifier.CODEC);
        runningEffect = input.read("running_effect", Identifier.CODEC).orElse(null);
        runningRunes = input.read("running_runes", RUNES_CODEC).orElse(runes);
        nextPayment = input.getLongOr("next_payment", 0);
        effectData = input.read("effect_data", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        // Sent only to clients.
        input.read("scan", SCAN_CODEC).ifPresent(received -> scan = received);
        instability = input.getIntOr("instability", instability);
        clientThreshold = input.getIntOr("threshold", clientThreshold);
        clientCapacity = input.getIntOr("capacity", clientCapacity);
        clientUpkeep = input.read("upkeep", Upkeep.CODEC);
        clientEffect = input.read("effect", Identifier.CODEC);
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
        return tag;
    }
}
