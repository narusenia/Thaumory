package one.nxeu.thaumory.circle.effect;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;

/**
 * Searing used from an item or a scroll: the circle's scorch around the user, on every hostile mob
 * within {@code radius} + level. Nothing is paid with none in reach (requirements §10.2).
 */
final class SearingInfusion implements InfusionEffect {
    @Override
    public boolean active() {
        return true;
    }

    @Override
    public boolean canUse(InfusionContext context) {
        return !targets(context).isEmpty();
    }

    @Override
    public void use(InfusionContext context) {
        float damage = (float) (context.setting("damage_per_level", 4) * context.infusionLevel());
        float burn = (float) context.setting("burn_seconds", 3);
        for (LivingEntity entity : targets(context)) {
            SearingEffect.sear(context.level(), entity, damage, burn);
        }
    }

    private static List<LivingEntity> targets(InfusionContext context) {
        double radius = context.setting("radius", 4) + context.infusionLevel();
        return context.level().getEntitiesOfClass(LivingEntity.class, context.wearer().getBoundingBox().inflate(radius),
                e -> e.isAlive() && e instanceof Enemy && e.distanceTo(context.wearer()) <= radius);
    }
}
