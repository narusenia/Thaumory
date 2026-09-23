package one.nxeu.thaumory.circle.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/** Herba + Vita, sustained. With Herba it helps crops along; with Bestia it brings young animals on. */
final class GrowthEffect implements CircleEffect {
    @Override
    public void apply(CircleContext context) {
        if (context.parameter().filter(ThaumoryAspects.HERBA::equals).isPresent()) {
            crops(context);
        } else if (context.parameter().filter(ThaumoryAspects.BESTIA::equals).isPresent()) {
            animals(context);
        }
    }

    /** One random tick's worth of growth for a crop in each of a few random columns. */
    private static void crops(CircleContext context) {
        ServerLevel level = context.level();
        int radius = CircleRange.blocks(context);
        CircleRange.randomColumns(context, (int) Math.ceil(4 * context.strength()), column -> {
            for (int dy = -radius; dy <= radius; dy++) {
                BlockPos pos = column.above(dy);
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.CROPS) && state.isRandomlyTicking()) {
                    state.randomTick(level, pos, level.getRandom());
                    return;
                }
            }
        });
    }

    private static void animals(CircleContext context) {
        int ticks = (int) Math.round(20 * context.strength());
        for (AgeableMob mob : context.level().getEntitiesOfClass(AgeableMob.class, CircleRange.box(context), mob -> mob.getAge() < 0)) {
            mob.setAge(Math.min(0, mob.getAge() + ticks));
        }
    }
}
