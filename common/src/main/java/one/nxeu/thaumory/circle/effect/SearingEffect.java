package one.nxeu.thaumory.circle.effect;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Bellum + Ignis, triggered. Scorches every hostile mob in range: fire damage of {@code
 * damage_per_level} × strength, and sets them burning for {@code burn_seconds}. Does not go off,
 * and costs nothing, with no hostile mob in range (requirements §17.4).
 */
final class SearingEffect implements CircleEffect {
    @Override
    public boolean canApply(CircleContext context) {
        return !targets(context).isEmpty();
    }

    @Override
    public void apply(CircleContext context) {
        ServerLevel level = context.level();
        float damage = (float) (context.setting("damage_per_level", 4) * context.strength());
        float burn = (float) context.setting("burn_seconds", 3);
        for (LivingEntity entity : targets(context)) {
            entity.hurtServer(level, level.damageSources().inFire(), damage);
            entity.igniteForSeconds(burn);
            level.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                    12, entity.getBbWidth() / 2, entity.getBbHeight() / 3, entity.getBbWidth() / 2, 0.02);
            context.affected(entity);
        }
    }

    private static List<LivingEntity> targets(CircleContext context) {
        return context.level().getEntitiesOfClass(LivingEntity.class, CircleRange.box(context), e -> e.isAlive() && e instanceof Enemy);
    }
}
