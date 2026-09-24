package one.nxeu.thaumory.circle.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Aqua + Herba, sustained. Sweeps the range wetting farmland through and putting out fires, and
 * douses anything living in range that is burning (requirements §17.4).
 */
final class MoistureEffect implements CircleEffect {
    @Override
    public void apply(CircleContext context) {
        ServerLevel level = context.level();
        CircleRange.sweep(context, pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FarmlandBlock && state.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE) {
                level.setBlock(pos, state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
                context.affected(pos);
            } else if (state.is(BlockTags.FIRE)) {
                level.removeBlock(pos, false);
                level.levelEvent(LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
                context.affected(pos);
            }
        });
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, CircleRange.box(context), LivingEntity::isOnFire)) {
            entity.clearFire();
            context.affected(entity);
        }
    }
}
