package one.nxeu.thaumory.block.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.circle.CircleDefinitions;
import one.nxeu.thaumory.circle.CircleIndex;
import one.nxeu.thaumory.circle.CircleMode;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.circle.CircleSide;
import one.nxeu.thaumory.circle.CircleUpkeep;
import one.nxeu.thaumory.flux.FluxSettings;
import one.nxeu.thaumory.flux.FluxWorldEffects;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.text.ThaumoryText;

/**
 * A magic circle's Core: up to three runes, kept by aspect id in slot order, the Essentia poured
 * in for them, and the chalk around it as last scanned. The scan is not saved; it runs again every
 * {@code scan_interval} ticks and goes to clients when it changes, for the loupe.
 *
 * <p>A sustained circle, once started, pays its upkeep on its interval and works once a second
 * until it is stopped, runs dry, or its circle breaks.
 *
 * <p>Trying an undefined combination releases Flux and records the failure; every payment of an
 * unstable circle may release Flux too. Flux around the Core makes it less stable from
 * manifestation on, and at overload any payment may make it misfire and explode.
 */
public final class CoreBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;
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

    /** {@code UNDEFINED}: fewer than two runes, so not a circle yet. {@code MISFIRED}: the combination is undefined. */
    /** {@code OVERLOADED}: it paid, but Flux at overload made it misfire and explode. */
    public enum StartResult { STARTED, ALREADY_RUNNING, NO_RINGS, UNDEFINED, MISFIRED, TRIGGERED_ONLY, NO_ESSENTIA, OVERLOADED }

    public enum TriggerResult { TRIGGERED, NO_RINGS, UNDEFINED, MISFIRED, SUSTAINED_ONLY, NO_TARGET, NO_ESSENTIA, OVERLOADED }

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
        instability = settings.instability(scan.nodes()) + (level instanceof ServerLevel server
                ? Thaumory.flux().settings().effects().extraInstability(fluxStage(server)) : 0);
        if (!level.isClientSide()) {
            stopIfBroken();
            updateIndex();
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
            updateIndex();
            sync();
        }
    }

    /** Keeps this Core listed under its combination while its circle works, so others can find it. */
    private void updateIndex() {
        if (level instanceof ServerLevel server) {
            CircleIndex index = CircleIndex.of(server);
            Optional<CircleCombination> combination = combination().filter(c -> circle().isPresent());
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

    /** The one who started or triggered the circle has now seen it work; the first time, they are told what it is. */
    private void recordSuccess(Optional<Entity> activator, Identifier effect) {
        if (!(activator.orElse(null) instanceof ServerPlayer player) || combination().isEmpty()) {
            return;
        }
        CircleCombination combination = combination().get();
        boolean known = Thaumory.knowledge().get(player).circle(combination).filter(PlayerKnowledge.CircleOutcome.SUCCESS::equals).isPresent();
        Thaumory.knowledge().update(player, k -> k.withCircle(combination, PlayerKnowledge.CircleOutcome.SUCCESS));
        if (!known) {
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

    /** Each payment of a circle over its instability threshold may leak Flux. */
    private void rollInstability(ServerLevel server) {
        releaseFlux(server, settings.instabilityFlux(instability, server.getRandom().nextDouble()));
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
        server.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.4f);
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
            return misfire(server, activator) ? StartResult.MISFIRED : StartResult.UNDEFINED;
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
        rollInstability(server);
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

    /** Stops a sustained circle, letting its effect clean up. False if it was not running. */
    public boolean stop() {
        if (running.isEmpty() || !(level instanceof ServerLevel server)) {
            return false;
        }
        Identifier effectId = runningEffect;
        Optional<Circle> circle = circle();
        CircleContext context = new Context(server, circle.flatMap(Circle::parameter),
                circle.map(c -> c.multipliers().strength()).orElse(1.0),
                circle.map(c -> settings.radius(scan.rings(), c.multipliers())).orElse(0.0), Optional.empty(),
                circle.map(c -> c.definition().settings()).orElse(Map.of()));
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
            return misfire(server, activator) ? TriggerResult.MISFIRED : TriggerResult.UNDEFINED;
        }
        CircleDefinitions.Definition definition = circle.get().definition();
        if (definition.mode() != CircleMode.TRIGGERED) {
            return TriggerResult.SUSTAINED_ONLY;
        }
        Optional<CircleEffect> effect = effect(definition.effect());
        CircleContext context = context(server, circle.get(), activator);
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
        rollInstability(server);
        effect.get().apply(context);
        recordSuccess(activator, definition.effect());
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
            if (misfire(server)) {
                stop();
                return;
            }
            rollInstability(server);
            nextPayment = time + CircleUpkeep.sustainedInterval(definition.interval(), circle.get().multipliers().cost());
            setChanged();
        }
        Optional<CircleEffect> effect = effect(runningEffect);
        if (effect.isPresent() && Math.floorMod(time + worldPosition.hashCode(), Math.max(1, effect.get().period())) == 0) {
            effect.get().apply(context(server, circle.get(), Optional.empty()));
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
        return new Context(server, circle.parameter(), circle.multipliers().strength(),
                settings.radius(scan.rings(), circle.multipliers()), activator, circle.definition().settings());
    }

    private final class Context implements CircleContext {
        private final ServerLevel level;
        private final Optional<Aspect> parameter;
        private final double strength;
        private final double radius;
        private final Optional<Entity> activator;
        private final Map<String, Double> numbers;

        Context(ServerLevel level, Optional<Aspect> parameter, double strength, double radius, Optional<Entity> activator,
                Map<String, Double> numbers) {
            this.level = level;
            this.parameter = parameter;
            this.strength = strength;
            this.radius = radius;
            this.activator = activator;
            this.numbers = numbers;
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
        public Optional<Aspect> parameter() {
            return parameter;
        }

        @Override
        public double strength() {
            return strength;
        }

        @Override
        public double radius() {
            return radius;
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
            return numbers.getOrDefault(key, fallback);
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
