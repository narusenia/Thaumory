package one.nxeu.thaumory.wand.spell;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;

/**
 * Tempestas (requirements §17.7): a bolt to the living thing the caster looks at, which then jumps to
 * the nearest hostile mobs, each for half. No lightning strikes, so nothing burns or changes.
 */
final class LightningSpell implements FocusSpell {
    @Override
    public boolean cast(FocusContext context) {
        Player caster = context.caster();
        ServerLevel level = context.level();
        double range = context.setting("range", 24);
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, eye, end,
                caster.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                entity -> entity instanceof LivingEntity living && living.isAlive() && !entity.isSpectator(), eye.distanceToSqr(end));
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target) || !context.pay()) {
            return false;
        }
        float damage = (float) (context.setting("damage", 5) * context.power());
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.LIGHTNING_BOLT),
                caster);
        strike(level, source, SpellTargets.tip(caster), target, damage);
        double radius = context.setting("chain_radius", 4);
        int count = (int) context.setting("chain_count", 2);
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius * count),
                entity -> entity != target && entity != caster && entity.isAlive() && entity instanceof Enemy);
        Entity last = target;
        for (int index : SpellMath.chain(SpellTargets.point(target.position()), hostiles.stream().map(e -> SpellTargets.point(e.position())).toList(),
                radius, count)) {
            LivingEntity next = hostiles.get(index);
            strike(level, source, last.getBoundingBox().getCenter(), next, damage / 2);
            last = next;
        }
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    private static void strike(ServerLevel level, DamageSource source, Vec3 from, LivingEntity target, float damage) {
        SpellTargets.line(level, ParticleTypes.ELECTRIC_SPARK, from, target.getBoundingBox().getCenter());
        target.hurtServer(level, source, damage);
    }
}
