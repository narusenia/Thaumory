package one.nxeu.thaumory.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.flux.pollution.PollutedBlock;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlock;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlock;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.block.pipe.EssentiaPipeBlock;
import one.nxeu.thaumory.block.pipe.FilterPipeBlock;
import one.nxeu.thaumory.block.pipe.PipeBlockEntity;
import one.nxeu.thaumory.block.pipe.PumpBlock;
import one.nxeu.thaumory.block.pipe.ValveBlock;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.sound.ThaumorySounds;

/** Blocks and their block entities. Block items are registered in {@link one.nxeu.thaumory.item.ThaumoryItems}. */
public final class ThaumoryBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumory.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Thaumory.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<CrucibleBlock> CRUCIBLE = register("crucible", CrucibleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops()
                    .strength(2.0f).sound(SoundType.METAL).noOcclusion());

    public static final RegistrySupplier<JarBlock> JAR = register("jar", JarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(ThaumorySounds.JAR).noOcclusion());

    public static final RegistrySupplier<EssentiaPipeBlock> PIPE = register("pipe", EssentiaPipeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.3f).sound(SoundType.GLASS).noOcclusion());

    public static final RegistrySupplier<FilterPipeBlock> FILTER_PIPE = register("filter_pipe", FilterPipeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.3f).sound(SoundType.GLASS).noOcclusion());
    public static final RegistrySupplier<ValveBlock> VALVE = register("valve", ValveBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.3f).sound(SoundType.GLASS).noOcclusion());
    public static final RegistrySupplier<PumpBlock> PUMP = register("pump", PumpBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.5f).sound(SoundType.METAL).noOcclusion());

    /** The Core by rank (requirements §4.6): gold, arcane iron, aether silver. */
    public static final RegistrySupplier<CircleCoreBlock> CIRCLE_CORE = circleCore("circle_core", 1);
    public static final RegistrySupplier<CircleCoreBlock> ARCANE_IRON_CIRCLE_CORE = circleCore("arcane_iron_circle_core", 2);
    public static final RegistrySupplier<CircleCoreBlock> AETHER_SILVER_CIRCLE_CORE = circleCore("aether_silver_circle_core", 3);

    /** Grows on the stone of caves (requirements §17.3); drops shards like an amethyst cluster. */
    public static final RegistrySupplier<AmethystClusterBlock> ARCANE_CRYSTAL = register("arcane_crystal",
            properties -> new AmethystClusterBlock(7.0f, 10.0f, properties),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).forceSolidOn().noOcclusion()
                    .sound(SoundType.AMETHYST_CLUSTER).strength(1.5f).lightLevel(state -> 6).pushReaction(PushReaction.POPPED));

    public static final RegistrySupplier<ChalkPatternBlock> CHALK_LINE = pattern("chalk_line", ThaumoryItems.CHALK);
    public static final RegistrySupplier<ChalkPatternBlock> AMPLIFYING_PATTERN = pattern("amplifying_pattern", ThaumoryItems.AMPLIFYING_CHALK);
    public static final RegistrySupplier<ChalkPatternBlock> EXTENDING_PATTERN = pattern("extending_pattern", ThaumoryItems.EXTENDING_CHALK);
    public static final RegistrySupplier<ChalkPatternBlock> ECONOMIZING_PATTERN = pattern("economizing_pattern", ThaumoryItems.ECONOMIZING_CHALK);
    public static final RegistrySupplier<ChalkPatternBlock> STABILIZING_PATTERN = pattern("stabilizing_pattern", ThaumoryItems.STABILIZING_CHALK);

    public static final RegistrySupplier<PollutedBlock> POLLUTED_SOIL = register("polluted_soil", PollutedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5f).sound(SoundType.GRAVEL));
    public static final RegistrySupplier<PollutedBlock> POLLUTED_STONE = register("polluted_stone", PollutedBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(1.5f, 6.0f)
                    .sound(SoundType.STONE));

    public static final RegistrySupplier<BlockEntityType<JarBlockEntity>> JAR_ENTITY = BLOCK_ENTITIES.register(
            "jar", () -> new BlockEntityType<>(JarBlockEntity::new, Set.of(JAR.get())));

    public static final RegistrySupplier<BlockEntityType<CrucibleBlockEntity>> CRUCIBLE_ENTITY = BLOCK_ENTITIES.register(
            "crucible", () -> new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(CRUCIBLE.get())));

    public static final RegistrySupplier<BlockEntityType<CircleCoreBlockEntity>> CIRCLE_CORE_ENTITY = BLOCK_ENTITIES.register(
            "circle_core", () -> new BlockEntityType<>(CircleCoreBlockEntity::new, Set.of(CIRCLE_CORE.get(), ARCANE_IRON_CIRCLE_CORE.get(), AETHER_SILVER_CIRCLE_CORE.get())));

    public static final RegistrySupplier<BlockEntityType<PipeBlockEntity>> PIPE_ENTITY = BLOCK_ENTITIES.register(
            "pipe", () -> new BlockEntityType<>(PipeBlockEntity::new, Set.of(PIPE.get(), FILTER_PIPE.get(), VALVE.get(), PUMP.get())));

    private ThaumoryBlocks() {}

    public static void register() {
        BLOCKS.register();
        BLOCK_ENTITIES.register();
    }

    /** Chalk patterns drop nothing and wash away like redstone dust. */
    private static RegistrySupplier<ChalkPatternBlock> pattern(String name, Supplier<? extends Item> chalk) {
        return register(name, properties -> new ChalkPatternBlock(chalk, properties), BlockBehaviour.Properties.of()
                .mapColor(MapColor.NONE).noCollision().instabreak().noLootTable().sound(SoundType.CALCITE)
                .pushReaction(PushReaction.POPPED));
    }

    private static RegistrySupplier<CircleCoreBlock> circleCore(String name, int rank) {
        return register(name, properties -> new CircleCoreBlock(rank, properties), BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops().strength(1.5f, 6.0f).sound(SoundType.STONE).noOcclusion());
    }

    private static <B extends Block> RegistrySupplier<B> register(String name, Function<BlockBehaviour.Properties, B> factory,
            BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Thaumory.id(name));
        return BLOCKS.register(name, () -> factory.apply(properties.setId(key)));
    }
}
