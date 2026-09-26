package one.nxeu.thaumory.wand.spell;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.particle.ThaumoryParticles;

/**
 * Lux (requirements §17.7): puts an invisible light where the caster looks, for good. Sneaking, it
 * takes away a light at that spot or right next to it, for free.
 */
final class LightSpell implements FocusSpell {
    private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);

    @Override
    public boolean cast(FocusContext context) {
        Player caster = context.caster();
        ServerLevel level = context.level();
        HitResult hit = caster.pick(context.setting("range", 32), 1.0f, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos spot = block.getBlockPos().relative(block.getDirection());
        if (!level.mayInteract(caster, spot)) {
            return false;
        }
        if (caster.isSecondaryUseActive()) {
            return takeAway(level, caster, spot);
        }
        if (!level.getBlockState(spot).isAir() || !context.pay()) {
            return false;
        }
        level.setBlock(spot, LIGHT, Block.UPDATE_ALL);
        trail(level, caster, Vec3.atCenterOf(spot));
        level.playSound(null, spot, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    /** Removes the light nearest {@code spot} within one block of it, if there is one. */
    private static boolean takeAway(ServerLevel level, Player caster, BlockPos spot) {
        Optional<BlockPos> found = StreamSupport.stream(BlockPos.betweenClosed(spot.offset(-1, -1, -1), spot.offset(1, 1, 1)).spliterator(), false)
                .filter(pos -> level.getBlockState(pos).is(Blocks.LIGHT))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(spot)));
        if (found.isEmpty()) {
            return false;
        }
        level.setBlock(found.get(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, found.get(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        return true;
    }

    /** Lux motes from the caster's eyes to where the light went, bursting there. */
    private static void trail(ServerLevel level, Player caster, Vec3 to) {
        ColorParticleOption mote = ColorParticleOption.create(ThaumoryParticles.ASPECT_MOTE.get(), 0xFF000000 | ThaumoryAspects.LUX.color());
        Vec3 from = caster.getEyePosition().add(caster.getLookAngle().scale(0.6));
        Vec3 step = to.subtract(from);
        int count = Math.max(2, (int) (step.length() * 2));
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.add(step.scale((double) i / count));
            level.sendParticles(mote, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
        }
        level.sendParticles(mote, to.x, to.y, to.z, 12, 0.25, 0.25, 0.25, 0);
    }
}
