package one.nxeu.thaumory.circle.effect;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import one.nxeu.thaumory.circle.ColumnSweep;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.circle.CircleContext;

/** A circle's range: the cube around its centre reaching {@code radius} blocks out (requirements §4.5). */
final class CircleRange {
    private static final String SWEEP = "sweep";

    private CircleRange() {}

    static AABB box(CircleContext context) {
        return new AABB(context.centre()).inflate(context.radius());
    }

    static int blocks(CircleContext context) {
        return (int) Math.floor(context.radius());
    }

    /** Up to {@code items_per_level} × strength, rounded up: how many items an item-handling circle deals with a second. */
    static int itemsPerCall(CircleContext context) {
        return (int) Math.ceil(context.setting("items_per_level", 4) * context.strength());
    }

    /** Dropped items in range that are still there, nearest the centre first. */
    static List<ItemEntity> items(CircleContext context, Predicate<ItemEntity> filter) {
        Vec3 centre = centre(context);
        return context.level().getEntitiesOfClass(ItemEntity.class, box(context), item -> item.isAlive() && filter.test(item)).stream()
                .sorted(Comparator.comparingDouble(item -> item.distanceToSqr(centre)))
                .toList();
    }

    static Vec3 centre(CircleContext context) {
        return Vec3.atBottomCenterOf(context.centre());
    }

    /**
     * Whether an effect that works every {@code period} ticks should mark what it reaches this
     * time: about once a second, so fast effects do not bury their targets in motes.
     */
    static boolean marksNow(CircleContext context, int period) {
        return context.level().getGameTime() % 20 < period;
    }

    /**
     * Visits every block of the next few columns in a sweep across the range, centre height ± range,
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
                visit.accept(context.centre().offset(column.dx(), dy, column.dz()));
            }
        }
    }

    /** Visits {@code columns} random columns of the range, each at the centre offset sideways. */
    static void randomColumns(CircleContext context, int columns, Consumer<BlockPos> visit) {
        int radius = blocks(context);
        var random = context.level().getRandom();
        for (int i = 0; i < columns; i++) {
            visit.accept(context.centre().offset(random.nextIntBetweenInclusive(-radius, radius), 0, random.nextIntBetweenInclusive(-radius, radius)));
        }
    }
}
