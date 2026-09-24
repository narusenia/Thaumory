package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CircleIndex;
import one.nxeu.thaumory.knowledge.CircleCombination;

/**
 * Arcanum + Aer, triggered. Carries whoever set it off to the nearest circle in the same dimension
 * with the same runes, slot 3 being the channel. Nothing happens, at no cost, if there is none.
 */
final class TeleportEffect implements CircleEffect {
    @Override
    public boolean canApply(CircleContext context) {
        return context.activator().isPresent() && destination(context).isPresent();
    }

    @Override
    public void apply(CircleContext context) {
        Optional<Entity> traveller = context.activator();
        Optional<BlockPos> target = destination(context);
        if (traveller.isEmpty() || target.isEmpty()) {
            return;
        }
        ServerLevel level = context.level();
        Entity entity = traveller.get();
        level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        BlockPos pos = target.get();
        entity.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot(), true);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** The nearest partner that really is still there and still working, dropping stale entries on the way. */
    private static Optional<BlockPos> destination(CircleContext context) {
        ServerLevel level = context.level();
        if (!(level.getBlockEntity(context.core()) instanceof CircleCoreBlockEntity here)) {
            return Optional.empty();
        }
        Optional<CircleCombination> combination = here.combination();
        if (combination.isEmpty()) {
            return Optional.empty();
        }
        CircleIndex index = CircleIndex.of(level);
        for (BlockPos pos : index.nearest(combination.get(), context.core())) {
            if (level.getBlockEntity(pos) instanceof CircleCoreBlockEntity there) {
                there.rescan();
                if (there.combination().equals(combination) && there.effectId().isPresent()) {
                    return Optional.of(pos);
                }
            }
            index.remove(pos);
        }
        return Optional.empty();
    }
}
