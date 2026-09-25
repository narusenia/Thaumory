package one.nxeu.thaumory.block.stone;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.api.essentia.EssentiaContainer;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleMishaps;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.circle.CircleDefinitions;
import one.nxeu.thaumory.circle.CircleMode;
import one.nxeu.thaumory.circle.CircleUpkeep;
import one.nxeu.thaumory.circle.WorkFlux;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.particle.ThaumoryParticles;

/**
 * A placed circle stone (requirements §10.3): the circle burnt into it, and the Essentia it holds
 * for that circle's aspects. While it holds enough and has no redstone signal, it runs the circle's
 * sustained effect around itself, {@code stone_radius} blocks out at strength 1, paying its upkeep
 * like a sustained circle. It has no rings, modifiers or rank, and so no instability; at overload a
 * payment may still misfire.
 */
public final class CircleStoneBlockEntity extends BlockEntity {
    private static final Codec<AspectList> ESSENTIA_CODEC = AspectCodecs.aspectList(ThaumoryApi.aspects());
    private static final String WORK_FLUX = "work_flux";

    /** Why a stone is or is not working, for the loupe. */
    public enum Status { RUNNING, POWERED, NO_ESSENTIA, UNDEFINED }

    private Optional<BurntCircle> burnt = Optional.empty();
    private AspectList essentia = AspectList.empty();
    /** The running combination's definition id, or empty while resting. */
    private Optional<Identifier> running = Optional.empty();
    private long nextPayment;
    private CompoundTag effectData = new CompoundTag();
    private double workFlux;
    private Status status = Status.UNDEFINED;

    public CircleStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.CIRCLE_STONE_ENTITY.get(), pos, state);
    }

    /**
     * Whether a circle stone can take {@code definition}: a sustained combination a rank 1 Core runs,
     * other than charging, which fills the item on a pedestal.
     */
    public static boolean takes(CircleDefinitions.Definition definition) {
        return definition.mode() == CircleMode.SUSTAINED && definition.rank() <= 1 && !definition.effect().equals(ThaumoryCircleEffects.CHARGING)
                && ThaumoryApi.circleEffects().get(definition.effect()).isPresent();
    }

    /** The definition the burnt combination has now, if the datapacks still define it and a stone can take it. */
    public static Optional<CircleDefinitions.Definition> definition(CircleCombination combination) {
        Optional<Aspect> first = ThaumoryApi.aspects().get(combination.first());
        Optional<Aspect> second = ThaumoryApi.aspects().get(combination.second());
        Optional<Aspect> parameter = combination.parameter().flatMap(ThaumoryApi.aspects()::get);
        Optional<Aspect> fourth = combination.slot4().flatMap(ThaumoryApi.aspects()::get);
        if (first.isEmpty() || second.isEmpty() || parameter.isEmpty() != combination.parameter().isEmpty()
                || fourth.isEmpty() != combination.slot4().isEmpty()) {
            return Optional.empty();
        }
        return CircleDefinitionReloadListener.definitions().find(first.get(), second.get(), parameter, fourth)
                .filter(CircleStoneBlockEntity::takes);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CircleStoneBlockEntity stone) {
        if (level instanceof ServerLevel server) {
            stone.tick(server, state, level.getGameTime());
        }
    }

    private Optional<CircleDefinitions.Definition> definition() {
        return burnt.flatMap(circle -> definition(circle.combination()));
    }

    private void tick(ServerLevel server, BlockState state, long time) {
        Optional<CircleDefinitions.Definition> definition = definition();
        boolean powered = state.getValue(CircleStoneBlock.POWERED);
        if (running.isPresent() && (definition.isEmpty() || !definition.get().id().equals(running.get()) || powered)) {
            stop(server);
        }
        if (definition.isEmpty()) {
            setStatus(Status.UNDEFINED);
            return;
        }
        if (powered) {
            setStatus(Status.POWERED);
            return;
        }
        CircleDefinitions.Definition circle = definition.get();
        if (time >= nextPayment) {
            Optional<AspectList> paid = CircleUpkeep.paySustained(essentia, circle.first(), circle.second());
            if (paid.isEmpty()) {
                stop(server);
                setStatus(Status.NO_ESSENTIA);
                return;
            }
            setEssentia(paid.get());
            // A misfire also waits out the interval, so a stone at overload does not blow up every tick.
            nextPayment = time + CircleUpkeep.sustainedInterval(circle.interval(), 1);
            if (CircleMishaps.overload(server, worldPosition)) {
                stop(server);
                return;
            }
            if (running.isEmpty()) {
                running = Optional.of(circle.id());
                effectData = new CompoundTag();
                effect(circle).ifPresent(effect -> effect.apply(new Context(server, circle)));
            }
            setChanged();
        }
        if (running.isEmpty()) {
            return;
        }
        setStatus(Status.RUNNING);
        Optional<CircleEffect> effect = effect(circle);
        if (effect.isPresent() && Math.floorMod(time + worldPosition.hashCode(), Math.max(1, effect.get().period())) == 0) {
            effect.get().apply(new Context(server, circle));
            setChanged();
        }
    }

    private static Optional<CircleEffect> effect(CircleDefinitions.Definition definition) {
        return ThaumoryApi.circleEffects().get(definition.effect());
    }

    /** Lets the running effect clean up. The stone starts again by itself once it can. */
    private void stop(ServerLevel server) {
        if (running.isEmpty()) {
            return;
        }
        // A combination the datapacks no longer define the same has nothing to clean up with.
        definition().filter(d -> d.id().equals(running.get()))
                .ifPresent(d -> effect(d).ifPresent(effect -> effect.stop(new Context(server, d))));
        running = Optional.empty();
        effectData = new CompoundTag();
        setChanged();
        sync();
    }

    private void setStatus(Status updated) {
        if (status != updated) {
            status = updated;
            sync();
        }
    }

    public Optional<BurntCircle> burnt() {
        return burnt;
    }

    /** What was last sent on the client. */
    public Status status() {
        return status;
    }

    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    // Essentia

    /** Every aspect of the burnt combination goes in, each up to the Core's capacity; pipes take nothing out. */
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

    public Set<Aspect> acceptedAspects() {
        return burnt.map(circle -> {
            CircleCombination combination = circle.combination();
            return Stream.concat(Stream.of(combination.first(), combination.second()),
                            Stream.concat(combination.parameter().stream(), combination.slot4().stream()))
                    .flatMap(id -> ThaumoryApi.aspects().get(id).stream())
                    .collect(Collectors.toUnmodifiableSet());
        }).orElse(Set.of());
    }

    public static int capacity() {
        return CircleCoreBlockEntity.capacity();
    }

    /** What an effect sees of this stone: itself as the centre, strength 1 and the stone's radius. */
    private final class Context implements CircleContext {
        private final ServerLevel level;
        private final CircleDefinitions.Definition definition;

        Context(ServerLevel level, CircleDefinitions.Definition definition) {
            this.level = level;
            this.definition = definition;
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
            return burnt.flatMap(circle -> circle.combination().parameter()).flatMap(ThaumoryApi.aspects()::get);
        }

        @Override
        public double strength() {
            return 1;
        }

        @Override
        public double radius() {
            return CircleCoreBlockEntity.settings().stoneRadius();
        }

        @Override
        public Optional<Entity> activator() {
            return Optional.empty();
        }

        @Override
        public CompoundTag data() {
            return effectData;
        }

        @Override
        public double setting(String key, double fallback) {
            return definition.settings().get(key) instanceof Double value ? value : fallback;
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
            if (amount <= 0) {
                return true;
            }
            Optional<AspectList> paid = CircleUpkeep.payWork(essentia, definition.first(), definition.second(), amount, 1);
            paid.ifPresent(CircleStoneBlockEntity.this::setEssentia);
            return paid.isPresent();
        }

        private List<Aspect> colours() {
            return List.of(definition.first(), definition.second());
        }
    }

    /** The Flux a working stone gives off (requirements §4.5), held back until it comes to a whole unit. */
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

    // Breaking, saving and syncing

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            stop(server);
            if (essentia.total() > 0) {
                ThaumoryApi.flux().add(server, ChunkPos.containing(pos), essentia.total());
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        burnt = Optional.ofNullable(components.get(ThaumoryComponents.BURNT_CIRCLE.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        burnt.ifPresent(circle -> components.set(ThaumoryComponents.BURNT_CIRCLE.get(), circle));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("burnt_circle");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        burnt.ifPresent(circle -> output.store("burnt_circle", BurntCircle.CODEC, circle));
        if (!essentia.isEmpty()) {
            output.store("essentia", ESSENTIA_CODEC, essentia);
        }
        running.ifPresent(id -> {
            output.store("running", Identifier.CODEC, id);
            output.putLong("next_payment", nextPayment);
        });
        if (!effectData.isEmpty()) {
            output.store("effect_data", CompoundTag.CODEC, effectData);
        }
        if (workFlux > 0) {
            output.putDouble("work_flux", workFlux);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnt = input.read("burnt_circle", BurntCircle.CODEC);
        essentia = input.read("essentia", ESSENTIA_CODEC).orElse(AspectList.empty());
        running = input.read("running", Identifier.CODEC);
        nextPayment = input.getLongOr("next_payment", 0);
        effectData = input.read("effect_data", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        workFlux = input.getDoubleOr("work_flux", 0);
        // Sent only to clients.
        input.getString("status").map(Status::valueOf).ifPresent(received -> status = received);
    }

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("effect_data");
        tag.putString("status", status.name());
        return tag;
    }
}
