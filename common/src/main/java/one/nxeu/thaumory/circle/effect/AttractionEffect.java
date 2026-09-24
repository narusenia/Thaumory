package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.data.ItemAspects;

/**
 * Tempestas + Vinculum, sustained. Draws dropped items in range towards the Core; with an aspect in
 * slot 3, only items that contain it.
 */
final class AttractionEffect implements CircleEffect {
    private static final int PERIOD = 5;
    private static final double SPEED = 0.25;
    private static final double MAX_SPEED = 1.0;
    /** Items this close to the Core are left alone, so they settle instead of circling. */
    private static final double ARRIVED = 0.75;

    @Override
    public int period() {
        return PERIOD;
    }

    @Override
    public void apply(CircleContext context) {
        Optional<Aspect> filter = context.parameter();
        Vec3 target = CircleRange.centre(context).add(0, 0.5, 0);
        double speed = Math.min(MAX_SPEED, SPEED * context.strength());
        boolean mark = CircleRange.marksNow(context, PERIOD);
        for (ItemEntity item : context.level().getEntitiesOfClass(ItemEntity.class, CircleRange.box(context),
                item -> filter.isEmpty() || ItemAspects.get(item.getItem()).contains(filter.get()))) {
            Vec3 towards = target.subtract(item.position());
            if (towards.length() > ARRIVED) {
                Vec3 motion = towards.normalize().scale(speed);
                item.setDeltaMovement(motion.x, Math.max(motion.y, 0.1), motion.z);
                if (mark) {
                    context.affected(item);
                }
            }
        }
    }
}
