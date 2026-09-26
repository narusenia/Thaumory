package one.nxeu.thaumory.wand.spell;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;
import one.nxeu.thaumory.wand.spell.SpellMath.Point;

/**
 * Aqua (requirements §17.7): a wave of cold in a cone ahead. It slows and chills the living things in
 * it and skins still water over with frosted ice that melts again.
 */
final class FrostSpell implements FocusSpell {
    @Override
    public boolean cast(FocusContext context) {
        Player caster = context.caster();
        ServerLevel level = context.level();
        double range = context.setting("range", 6);
        double angle = context.setting("half_angle", 30);
        Vec3 eye = caster.getEyePosition();
        Point look = SpellTargets.point(caster.getLookAngle());
        List<LivingEntity> chilled = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(range),
                entity -> entity != caster && entity.isAlive()
                        && SpellMath.inCone(SpellTargets.point(entity.getBoundingBox().getCenter().subtract(eye)), look, range, angle));
        List<BlockPos> water = water(level, caster, range, angle);
        if ((chilled.isEmpty() && water.isEmpty()) || !context.pay()) {
            return false;
        }
        int ticks = (int) (context.setting("slow_seconds", 5) * context.power() * 20);
        for (LivingEntity entity : chilled) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 1), caster);
            entity.setTicksFrozen(entity.getTicksRequiredToFreeze());
            level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        }
        for (BlockPos pos : water) {
            level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
            level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(caster.getRandom(), 60, 120));
        }
        Vec3 tip = SpellTargets.tip(caster);
        for (int i = 0; i < 12; i++) {
            Vec3 out = caster.getLookAngle().scale(0.5).add(caster.getRandom().triangle(0, 0.15), caster.getRandom().triangle(0, 0.15),
                    caster.getRandom().triangle(0, 0.15));
            level.sendParticles(ParticleTypes.SNOWFLAKE, tip.x, tip.y, tip.z, 0, out.x, out.y, out.z, 1.0);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    /**
     * Water sources with air above, around the caster's feet, inside the cone as seen from above: water
     * lies below the eyes, so height would push it out of the cone.
     */
    private static List<BlockPos> water(ServerLevel level, Player caster, double range, double angle) {
        Point ahead = SpellTargets.point(Vec3.directionFromRotation(0, caster.getYHeadRot()));
        BlockPos feet = caster.blockPosition();
        int reach = (int) Math.ceil(range);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-reach, -2, -reach), feet.offset(reach, 1, reach))) {
            Point flat = new Point(pos.getX() + 0.5 - caster.getX(), 0, pos.getZ() + 0.5 - caster.getZ());
            if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource() && level.getBlockState(pos.above()).isAir()
                    && SpellMath.inCone(flat, ahead, range, angle)) {
                found.add(pos.immutable());
            }
        }
        return found;
    }
}
