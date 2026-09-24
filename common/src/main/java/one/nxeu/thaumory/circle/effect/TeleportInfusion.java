package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CircleIndex;
import one.nxeu.thaumory.knowledge.CircleCombination;

/** Teleport on an item, active: carries the wearer to the nearest working circle of the item's channel. */
final class TeleportInfusion implements InfusionEffect {
    @Override
    public boolean active() {
        return true;
    }

    @Override
    public boolean canUse(InfusionContext context) {
        return destination(context).isPresent();
    }

    @Override
    public void use(InfusionContext context) {
        destination(context).ifPresent(pos -> {
            ServerLevel level = context.level();
            LivingEntity entity = context.wearer();
            level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            entity.teleportTo(level, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, Set.of(), entity.getYRot(), entity.getXRot(), true);
            level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        });
    }

    private static Optional<BlockPos> destination(InfusionContext context) {
        if (context.parameter().isEmpty()) {
            return Optional.empty();
        }
        CircleCombination combination = new CircleCombination(ThaumoryAspects.ARCANUM.id(), ThaumoryAspects.AER.id(),
                Optional.of(context.parameter().get().id()));
        ServerLevel level = context.level();
        CircleIndex index = CircleIndex.of(level);
        for (BlockPos pos : index.nearest(combination, context.wearer().blockPosition())) {
            if (level.getBlockEntity(pos) instanceof CircleCoreBlockEntity there) {
                there.rescan();
                if (there.combination().filter(combination::equals).isPresent() && there.effectId().isPresent()) {
                    return Optional.of(pos);
                }
            }
            index.remove(pos);
        }
        return Optional.empty();
    }
}
