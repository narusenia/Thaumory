package one.nxeu.thaumory.circle.effect;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Bestia + Vinculum, sustained. Draws what slot 3 picks (hostile mobs when it is empty) towards the
 * Core, the ward's push turned inwards, {@code pull_per_level} × strength every quarter second
 * (requirements §17.4).
 */
final class LureEffect implements CircleEffect {
    private static final int PERIOD = 5;
    private static final double MAX_PULL = 1.0;
    /** Mobs this close to the Core are left alone, so they gather instead of piling onto it. */
    private static final double ARRIVED = 1.5;

    @Override
    public int period() {
        return PERIOD;
    }

    @Override
    public void apply(CircleContext context) {
        Predicate<Entity> target = AuraEffect.enemiesUnlessPicked(context.parameter());
        Vec3 centre = CircleRange.centre(context);
        double pull = Math.min(MAX_PULL, context.setting("pull_per_level", 0.6) * context.strength());
        boolean mark = CircleRange.marksNow(context, PERIOD);
        for (LivingEntity entity : context.level().getEntitiesOfClass(LivingEntity.class, CircleRange.box(context),
                e -> e.isAlive() && !(e instanceof Player) && target.test(e))) {
            Vec3 towards = centre.subtract(entity.position());
            if (towards.length() <= ARRIVED) {
                continue;
            }
            Vec3 motion = towards.normalize().scale(pull);
            entity.push(motion.x, Math.max(motion.y, 0) * 0.5, motion.z);
            if (mark) {
                context.affected(entity);
            }
        }
    }
}
