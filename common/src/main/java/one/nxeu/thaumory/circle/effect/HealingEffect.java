package one.nxeu.thaumory.circle.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/** Vita + Ordo, sustained. Heals everything living in range, or only animals with Bestia in slot 3. */
final class HealingEffect implements CircleEffect {
    @Override
    public void apply(CircleContext context) {
        boolean animalsOnly = context.parameter().filter(ThaumoryAspects.BESTIA::equals).isPresent();
        float amount = (float) context.strength();
        for (LivingEntity entity : context.level().getEntitiesOfClass(LivingEntity.class, CircleRange.box(context),
                e -> e.isAlive() && (!animalsOnly || e instanceof Animal))) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(amount);
                context.affected(entity);
            }
        }
    }
}
