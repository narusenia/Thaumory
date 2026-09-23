package one.nxeu.thaumory.block;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.crucible.CrucibleBlock;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlock;
import one.nxeu.thaumory.block.jar.JarBlockEntity;

/** Blocks and their block entities. Block items are registered in {@link one.nxeu.thaumory.item.ThaumoryItems}. */
public final class ThaumoryBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Thaumory.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Thaumory.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<CrucibleBlock> CRUCIBLE = register("crucible", CrucibleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops()
                    .strength(2.0f).sound(SoundType.METAL).noOcclusion());

    public static final RegistrySupplier<JarBlock> JAR = register("jar", JarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS).noOcclusion());

    public static final RegistrySupplier<BlockEntityType<JarBlockEntity>> JAR_ENTITY = BLOCK_ENTITIES.register(
            "jar", () -> new BlockEntityType<>(JarBlockEntity::new, Set.of(JAR.get())));

    public static final RegistrySupplier<BlockEntityType<CrucibleBlockEntity>> CRUCIBLE_ENTITY = BLOCK_ENTITIES.register(
            "crucible", () -> new BlockEntityType<>(CrucibleBlockEntity::new, Set.of(CRUCIBLE.get())));

    private ThaumoryBlocks() {}

    public static void register() {
        BLOCKS.register();
        BLOCK_ENTITIES.register();
    }

    private static <B extends Block> RegistrySupplier<B> register(String name, Function<BlockBehaviour.Properties, B> factory,
            BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Thaumory.id(name));
        return BLOCKS.register(name, () -> factory.apply(properties.setId(key)));
    }
}
