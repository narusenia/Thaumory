package one.nxeu.thaumory.circle.effect;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import one.nxeu.thaumory.circle.ColumnSweep;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.circle.CircleContext;

/** A circle's range: the cube around the Core reaching {@code radius} blocks out (requirements §4.5). */
final class CircleRange {
    private static final String SWEEP = "sweep";

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

    /**
     * Whether an effect that works every {@code period} ticks should mark what it reaches this
     * time: about once a second, so fast effects do not bury their targets in motes.
     */
    static boolean marksNow(CircleContext context, int period) {
        return context.level().getGameTime() % 20 < period;
    }

    /**
     * Visits every block of the next few columns in a sweep across the range, Core height ± range,
     * {@code columns_per_level} × strength columns a call (requirements §17.4). Where the sweep got
     * to is kept in the context's data.
     */
    static void sweep(CircleContext context, Consumer<BlockPos> visit) {
        int radius = blocks(context);
        int count = (int) Math.ceil(context.setting("columns_per_level", 8) * context.strength());
        ColumnSweep.Step step = ColumnSweep.next(radius, context.data().getIntOr(SWEEP, 0), count);
        context.data().putInt(SWEEP, step.cursor());
        for (ColumnSweep.Column column : step.columns()) {
            for (int dy = -radius; dy <= radius; dy++) {
                visit.accept(context.core().offset(column.dx(), dy, column.dz()));
            }
        }
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
