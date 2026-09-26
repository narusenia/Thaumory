package one.nxeu.thaumory.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * What the fire focus shoots (requirements §17.7): flies straight, burns the living thing it hits,
 * and puffs out on a block without setting it alight or breaking it.
 */
public final class FocusFireball extends ThrowableItemProjectile {
    /** Ticks it flies before it burns out. */
    private static final int LIFETIME = 60;

    private float damage = 4;
    private int burnSeconds = 4;

    public FocusFireball(EntityType<? extends FocusFireball> type, Level level) {
        super(type, level);
    }

    public FocusFireball(Level level, LivingEntity owner, float damage, int burnSeconds) {
        super(ThaumoryEntities.FOCUS_FIREBALL.get(), owner, level, new ItemStack(Items.FIRE_CHARGE));
        this.damage = damage;
        this.burnSeconds = burnSeconds;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.01);
            if (tickCount > LIFETIME) {
                discard();
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = result.getEntity();
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FIREBALL),
                this, getOwner());
        if (target.hurtServer(level, source, damage)) {
            target.igniteForSeconds(burnSeconds);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 8, 0.15, 0.15, 0.15, 0.02);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.4f);
            discard();
        }
    }
}
