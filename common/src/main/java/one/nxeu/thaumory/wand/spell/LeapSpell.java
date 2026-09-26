package one.nxeu.thaumory.wand.spell;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;

/**
 * Arcanum + Aer (requirements §17.7): moves the caster a short way along their gaze, stopping short of
 * walls and stepping back a block at a time until they fit.
 */
final class LeapSpell implements FocusSpell {
    @Override
    public boolean cast(FocusContext context) {
        Player caster = context.caster();
        ServerLevel level = context.level();
        Vec3 look = caster.getLookAngle();
        double reach = context.setting("distance", 8) * context.power();
        Vec3 eye = caster.getEyePosition();
        BlockHitResult wall = level.clip(new ClipContext(eye, eye.add(look.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (wall.getType() != HitResult.Type.MISS) {
            reach = wall.getLocation().distanceTo(eye) - 0.5;
        }
        Vec3 from = caster.position();
        for (double distance = Math.floor(reach); distance >= 1; distance--) {
            Vec3 to = from.add(look.scale(distance));
            if (level.noCollision(caster, caster.getBoundingBox().move(to.subtract(from)))) {
                if (!context.pay()) {
                    return false;
                }
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 20, 0.3, 0.6, 0.3, 0.02);
                caster.teleportTo(to.x, to.y, to.z);
                caster.resetFallDistance();
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 20, 0.3, 0.6, 0.3, 0.02);
                level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
                return true;
            }
        }
        return false;
    }
}
