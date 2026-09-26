package one.nxeu.thaumory.wand.spell;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;

/**
 * Terra + Bellum (requirements §17.7): breaks the block the caster looks at, from afar, dropping what it
 * would drop broken by hand. The wand's core sets how hard a block it can break.
 */
final class DiggingSpell implements FocusSpell {
    @Override
    public boolean cast(FocusContext context) {
        ServerLevel level = context.level();
        Optional<BlockHitResult> hit = SpellTargets.block(context.caster(), context.setting("range", 16));
        if (hit.isEmpty()) {
            return false;
        }
        BlockPos pos = hit.get().getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!SpellTargets.breakable(context, pos, state) || !context.pay()) {
            return false;
        }
        SpellTargets.line(level, ParticleTypes.CRIT, SpellTargets.tip(context.caster()), Vec3.atCenterOf(pos));
        level.destroyBlock(pos, true, context.caster());
        return true;
    }
}
