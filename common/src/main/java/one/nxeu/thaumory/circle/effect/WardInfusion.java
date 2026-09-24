package one.nxeu.thaumory.circle.effect;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;

/** Ward on an item, passive: pushes the creatures slot 3 names away from the wearer. */
final class WardInfusion implements InfusionEffect {
    private static final double PUSH = 0.4;

    @Override
    public int period() {
        return 5;
    }

    @Override
    public void tick(InfusionContext context) {
        push(context, context.setting("radius", 2) + context.infusionLevel(), PUSH);
    }

    @Override
    public boolean castable() {
        return true;
    }

    /** One strong shove over a wider reach. */
    @Override
    public boolean cast(InfusionContext context) {
        return push(context, context.setting("scroll_radius", 4) + context.infusionLevel(), context.setting("scroll_push", 1.5)) > 0;
    }

    /** Pushes the targets within {@code radius} away from the wearer; returns how many there were. */
    private static int push(InfusionContext context, double radius, double strength) {
        if (context.parameter().isEmpty()) {
            return 0;
        }
        Predicate<Entity> target = WardEffect.target(context.parameter().get());
        LivingEntity wearer = context.wearer();
        int pushed = 0;
        for (LivingEntity entity : context.level().getEntitiesOfClass(LivingEntity.class, wearer.getBoundingBox().inflate(radius),
                e -> e != wearer && !(e instanceof Player) && target.test(e))) {
            Vec3 away = entity.position().subtract(wearer.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) {
                away = new Vec3(1, 0, 0);
            }
            away = away.normalize().scale(strength);
            entity.push(away.x, 0.1, away.z);
            pushed++;
        }
        return pushed;
    }
}
