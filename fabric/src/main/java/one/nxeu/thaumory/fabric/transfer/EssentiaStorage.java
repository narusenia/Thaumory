package one.nxeu.thaumory.fabric.transfer;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.Direction;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.essentia.EssentiaContainer;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Essentia storages in the world, for pipes and other mods (requirements §8.2). The context is
 * the side being accessed; Thaumory's own blocks treat every side, and no side, the same.
 */
public final class EssentiaStorage {
    public static final BlockApiLookup<Storage<EssentiaVariant>, @Nullable Direction> SIDED =
            BlockApiLookup.get(Thaumory.id("essentia"), Storage.asClass(), Direction.class);

    /** One storage per container, so every lookup of a block shares its pending transfers. */
    private static final Map<EssentiaContainer, Storage<EssentiaVariant>> STORAGES = new WeakHashMap<>();

    private EssentiaStorage() {}

    /** The storage for a container, made on first use. For addons exposing their own containers. */
    public static synchronized Storage<EssentiaVariant> of(EssentiaContainer container) {
        return STORAGES.computeIfAbsent(container, ContainerStorage::new);
    }

    public static void register() {
        SIDED.registerForBlockEntities((entity, side) -> of(((CrucibleBlockEntity) entity).container()), ThaumoryBlocks.CRUCIBLE_ENTITY.get());
        SIDED.registerForBlockEntities((entity, side) -> of(((JarBlockEntity) entity).container()), ThaumoryBlocks.JAR_ENTITY.get());
        SIDED.registerForBlockEntities((entity, side) -> of(((CircleCoreBlockEntity) entity).container()), ThaumoryBlocks.CIRCLE_CORE_ENTITY.get());
    }
}
