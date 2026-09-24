package one.nxeu.thaumory.circle.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/** Growth on an item, passive: crops (Herba) or young animals (Bestia) around the wearer grow. */
final class GrowthInfusion implements InfusionEffect {
    @Override
    public void tick(InfusionContext context) {
        int radius = (int) (context.setting("radius", 2) + context.infusionLevel());
        if (context.parameter().filter(ThaumoryAspects.HERBA::equals).isPresent()) {
            crops(context, radius);
        } else if (context.parameter().filter(ThaumoryAspects.BESTIA::equals).isPresent()) {
            int ticks = (int) Math.round(context.setting("animal_ticks_per_level", 20) * context.infusionLevel());
            for (AgeableMob mob : context.level().getEntitiesOfClass(AgeableMob.class, context.wearer().getBoundingBox().inflate(radius),
                    mob -> mob.getAge() < 0)) {
                mob.setAge(Math.min(0, mob.getAge() + ticks));
            }
        }
    }

    @Override
    public boolean castable() {
        return true;
    }

    /** Every crop in reach gets a few random ticks at once, or young animals a good step towards grown. */
    @Override
    public boolean cast(InfusionContext context) {
        ServerLevel level = context.level();
        int radius = (int) (context.setting("scroll_radius", 3) + context.infusionLevel());
        BlockPos centre = context.wearer().blockPosition();
        if (context.parameter().filter(ThaumoryAspects.HERBA::equals).isPresent()) {
            int ticks = (int) Math.round(context.setting("scroll_ticks_per_level", 2) * context.infusionLevel());
            boolean grew = false;
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -2, -radius), centre.offset(radius, 2, radius))) {
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.CROPS) && state.isRandomlyTicking()) {
                    BlockPos crop = pos.immutable();
                    for (int i = 0; i < ticks; i++) {
                        level.getBlockState(crop).randomTick(level, crop, level.getRandom());
                    }
                    grew = true;
                }
            }
            return grew;
        }
        if (context.parameter().filter(ThaumoryAspects.BESTIA::equals).isPresent()) {
            int ticks = (int) Math.round(context.setting("scroll_animal_ticks_per_level", 1200) * context.infusionLevel());
            var young = level.getEntitiesOfClass(AgeableMob.class, context.wearer().getBoundingBox().inflate(radius), mob -> mob.getAge() < 0);
            young.forEach(mob -> mob.setAge(Math.min(0, mob.getAge() + ticks)));
            return !young.isEmpty();
        }
        return false;
    }

    /** One random tick for a crop in each of a few random columns around the wearer. */
    private static void crops(InfusionContext context, int radius) {
        ServerLevel level = context.level();
        RandomSource random = level.getRandom();
        BlockPos centre = context.wearer().blockPosition();
        int columns = (int) Math.round(context.setting("columns_per_level", 2) * context.infusionLevel());
        for (int i = 0; i < columns; i++) {
            BlockPos column = centre.offset(random.nextIntBetweenInclusive(-radius, radius), 0, random.nextIntBetweenInclusive(-radius, radius));
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos pos = column.above(dy);
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.CROPS) && state.isRandomlyTicking()) {
                    state.randomTick(level, pos, random);
                    break;
                }
            }
        }
    }
}
