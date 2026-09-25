package one.nxeu.thaumory.block.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.flux.FluxSettings;
import one.nxeu.thaumory.flux.FluxWorldEffects;
import one.nxeu.thaumory.sound.ThaumorySounds;

/** What goes wrong at a Core or a circle stone: Flux leaking out, and the blast of a payment at overload. */
public final class CircleMishaps {
    private CircleMishaps() {}

    /** Releases {@code amount} Flux into the chunk at {@code pos}, with a puff and a sound. */
    public static void releaseFlux(ServerLevel server, BlockPos pos, double amount) {
        if (amount <= 0) {
            return;
        }
        ThaumoryApi.flux().add(server, ChunkPos.containing(pos), amount);
        server.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 12, 0.6, 0.2, 0.6, 0);
        server.playSound(null, pos, ThaumorySounds.CIRCLE_FLUX.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    /**
     * At overload, a payment may misfire: the effect does not happen, the Essentia paid is lost,
     * Flux leaks out and a blast that breaks nothing hurts what is near and pollutes the ground.
     *
     * @return whether it misfired
     */
    public static boolean overload(ServerLevel server, BlockPos pos) {
        FluxSettings.Effects effects = Thaumory.flux().settings().effects();
        double chance = effects.misfireChance(Thaumory.flux().stage(server, ChunkPos.containing(pos)));
        if (chance <= 0 || server.getRandom().nextDouble() >= chance) {
            return false;
        }
        releaseFlux(server, pos, CircleCoreBlockEntity.settings().undefinedFlux());
        double radius = effects.explosionRadius();
        Vec3 center = Vec3.atCenterOf(pos);
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.5, center.z, 60, radius / 2, 0.5, radius / 2, 0.1);
        server.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(radius))) {
            Vec3 away = entity.position().subtract(center);
            if (away.length() > radius) {
                continue;
            }
            entity.hurtServer(server, server.damageSources().magic(), effects.explosionDamage());
            Vec3 push = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : away.normalize();
            entity.push(push.x, 0.4, push.z);
            entity.needsSync = true;
        }
        FluxWorldEffects.polluteAround(server, pos, radius);
        return true;
    }
}
