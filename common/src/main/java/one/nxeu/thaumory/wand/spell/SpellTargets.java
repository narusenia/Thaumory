package one.nxeu.thaumory.wand.spell;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.wand.spell.SpellMath.Point;

/** What several focus spells share: aiming at blocks, what may be broken, and trails of particles. */
final class SpellTargets {
    private SpellTargets() {}

    /** The block the caster looks at within {@code range}, passing through liquids. */
    static Optional<BlockHitResult> block(Player caster, double range) {
        HitResult hit = caster.pick(range, 1.0f, false);
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? Optional.of(block) : Optional.empty();
    }

    /**
     * Whether the caster's wand may break the block at {@code pos}: not air, not unbreakable, not
     * beyond its core's tier, and where the caster may build.
     */
    static boolean breakable(FocusContext context, BlockPos pos, BlockState state) {
        ServerLevel level = context.level();
        return !state.isAir() && state.getDestroySpeed(level, pos) >= 0 && !state.is(context.incorrectFor())
                && level.mayInteract(context.caster(), pos) && context.caster().mayBuild();
    }

    /** A line of {@code particle} from {@code from} to {@code to}, two a block. */
    static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int count = Math.max(2, (int) (step.length() * 2));
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.add(step.scale((double) i / count));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** Where the wand's tip is: a little in front of the caster's eyes. */
    static Vec3 tip(Player caster) {
        return caster.getEyePosition().add(caster.getLookAngle().scale(0.6));
    }

    static Point point(Vec3 vector) {
        return new Point(vector.x, vector.y, vector.z);
    }
}
