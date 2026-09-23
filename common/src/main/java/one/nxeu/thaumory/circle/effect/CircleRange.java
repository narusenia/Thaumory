package one.nxeu.thaumory.circle.effect;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.circle.CircleContext;

/** A circle's range: the cube around the Core reaching {@code radius} blocks out (requirements §4.5). */
final class CircleRange {
    private CircleRange() {}

    static AABB box(CircleContext context) {
        return new AABB(context.core()).inflate(context.radius());
    }

    static int blocks(CircleContext context) {
        return (int) Math.floor(context.radius());
    }

    static Vec3 centre(CircleContext context) {
        return Vec3.atBottomCenterOf(context.core());
    }

    /** Visits {@code columns} random columns of the range, each at the Core's position offset sideways. */
    static void randomColumns(CircleContext context, int columns, Consumer<BlockPos> visit) {
        int radius = blocks(context);
        var random = context.level().getRandom();
        for (int i = 0; i < columns; i++) {
            visit.accept(context.core().offset(random.nextIntBetweenInclusive(-radius, radius), 0, random.nextIntBetweenInclusive(-radius, radius)));
        }
    }
}
