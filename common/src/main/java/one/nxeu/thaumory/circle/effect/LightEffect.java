package one.nxeu.thaumory.circle.effect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/**
 * Lux + Ignis. With slot 3 empty it lights up dark ground in range with invisible light blocks,
 * which go away when the circle stops. With Umbra it keeps darkness on everything living in range.
 */
final class LightEffect implements CircleEffect {
    private static final String LIGHTS = "lights";
    private static final int DARK = 7;
    private static final int TRIES_PER_LIGHT = 8;
    private static final int DARKNESS_TICKS = 60;
    private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);

    @Override
    public void apply(CircleContext context) {
        if (context.parameter().filter(ThaumoryAspects.UMBRA::equals).isPresent()) {
            darken(context);
        } else {
            light(context);
        }
    }

    private static void darken(CircleContext context) {
        for (LivingEntity entity : context.level().getEntitiesOfClass(LivingEntity.class, range(context))) {
            entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS, 0, true, false));
        }
    }

    private static void light(CircleContext context) {
        ServerLevel level = context.level();
        RandomSource random = level.getRandom();
        int radius = (int) Math.floor(context.radius());
        int wanted = (int) Math.ceil(2 * context.strength());
        List<Long> placed = new ArrayList<>(Arrays.stream(context.data().getLongArray(LIGHTS).orElse(new long[0])).boxed().toList());
        int added = 0;
        for (int attempt = 0; attempt < wanted * TRIES_PER_LIGHT && added < wanted; attempt++) {
            BlockPos column = context.core().offset(random.nextIntBetweenInclusive(-radius, radius), 0, random.nextIntBetweenInclusive(-radius, radius));
            BlockPos spot = groundIn(level, column, radius);
            if (spot != null && level.getBrightness(LightLayer.BLOCK, spot) <= DARK) {
                level.setBlock(spot, LIGHT, Block.UPDATE_ALL);
                placed.add(spot.asLong());
                added++;
            }
        }
        if (added > 0) {
            context.data().putLongArray(LIGHTS, placed.stream().mapToLong(Long::longValue).toArray());
        }
    }

    /** The lowest air block above solid ground in the column, within the range's height. */
    private static BlockPos groundIn(ServerLevel level, BlockPos column, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            BlockPos pos = column.above(dy);
            BlockPos below = pos.below();
            if (level.getBlockState(pos).isAir() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return pos;
            }
        }
        return null;
    }

    @Override
    public void stop(CircleContext context) {
        ServerLevel level = context.level();
        for (long packed : context.data().getLongArray(LIGHTS).orElse(new long[0])) {
            BlockPos pos = BlockPos.of(packed);
            if (level.getBlockState(pos).is(Blocks.LIGHT)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static AABB range(CircleContext context) {
        return new AABB(context.core()).inflate(context.radius());
    }
}
