package one.nxeu.thaumory.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.ThaumoryApi;

/**
 * Flux sealed away by a containment circle (requirements §17.5). Safe in a container or an
 * inventory; a dropped crystal that is destroyed, falls out of the world, despawns or touches water
 * breaks, and its Flux goes back to the chunk it is in.
 */
public class FluxCrystalItem extends Item {
    /** What a crystal holds when nothing says otherwise. */
    public static final int DEFAULT_FLUX = 10;

    public FluxCrystalItem(Properties properties) {
        super(properties);
    }

    /** The Flux the whole stack holds. */
    public static int flux(ItemStack stack) {
        if (!(stack.getItem() instanceof FluxCrystalItem)) {
            return 0;
        }
        return stack.getOrDefault(ThaumoryComponents.SEALED_FLUX.get(), DEFAULT_FLUX) * stack.getCount();
    }

    /** Gives the stack's Flux to the chunk at {@code at}, with the sound and sparks of it breaking. */
    public static void release(ServerLevel level, Vec3 at, ItemStack stack) {
        int flux = flux(stack);
        if (flux <= 0) {
            return;
        }
        ThaumoryApi.flux().add(level, ChunkPos.containing(BlockPos.containing(at)), flux);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.2, at.z, 12, 0.25, 0.25, 0.25, 0.02);
    }

    /** Breaks a dropped stack: its Flux goes out and the item is gone. */
    public static void shatter(ItemEntity entity) {
        if (entity.level() instanceof ServerLevel level && !entity.isRemoved()) {
            release(level, entity.position(), entity.getItem());
            entity.setItem(ItemStack.EMPTY);
        }
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        // Burnt, blown up or pricked; the entity is discarded right after.
        if (entity.level() instanceof ServerLevel level) {
            release(level, entity.position(), entity.getItem());
        }
    }
}
