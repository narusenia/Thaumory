package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * with the same runes, slot 3 being the channel. It lands on the first footing down from that Core
 * (the Core itself on a floor; under a wall or ceiling circle, whatever is below). Nothing happens,
 * at no cost, if there is none.
 */
final class TeleportEffect implements CircleEffect {
    /** How far below a wall or ceiling Core a footing may be. */
    private static final int MAX_DROP = 32;

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
        context.showAffected(entity);
        BlockPos pos = target.get();
        entity.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot(), true);
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        context.showAffected(entity);
    }

    /**
     * Where to land at the nearest partner that really is still there and still working, dropping
     * stale entries on the way. A partner with nowhere to stand is passed over.
     */
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
                    Optional<BlockPos> footing = footing(level, pos);
                    if (footing.isPresent()) {
                        return footing;
                    }
                    continue;
                }
            }
            index.remove(pos);
        }
        return Optional.empty();
    }

    /** The first open cell at or below {@code core} that stands on a sturdy top face. */
    private static Optional<BlockPos> footing(ServerLevel level, BlockPos core) {
        BlockPos.MutableBlockPos pos = core.mutable();
        for (int drop = 0; drop <= MAX_DROP && pos.getY() > level.getMinY(); drop++, pos.move(Direction.DOWN)) {
            BlockPos below = pos.below();
            boolean open = drop == 0 || level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            if (open && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return Optional.of(pos.immutable());
            }
            if (!open) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
