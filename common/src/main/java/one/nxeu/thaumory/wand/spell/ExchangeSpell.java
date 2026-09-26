package one.nxeu.thaumory.wand.spell;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;

/**
 * Ordo + Chaos (requirements §17.7): swaps the block the caster looks at for the block in their off
 * hand. What the old block drops goes into the inventory. Blocks holding anything are left alone.
 */
final class ExchangeSpell implements FocusSpell {
    @Override
    public boolean cast(FocusContext context) {
        Player caster = context.caster();
        ServerLevel level = context.level();
        ItemStack held = caster.getOffhandItem();
        Optional<BlockHitResult> hit = SpellTargets.block(caster, context.setting("range", 6));
        if (!(held.getItem() instanceof BlockItem blockItem) || hit.isEmpty()) {
            return false;
        }
        BlockPos pos = hit.get().getBlockPos();
        BlockState old = level.getBlockState(pos);
        BlockState replacement = Block.updateFromNeighbourShapes(blockItem.getBlock().defaultBlockState(), level, pos);
        if (old.is(blockItem.getBlock()) || level.getBlockEntity(pos) != null || !SpellTargets.breakable(context, pos, old)
                || !replacement.canSurvive(level, pos) || !context.pay()) {
            return false;
        }
        for (ItemStack drop : Block.getDrops(old, level, pos, null, caster, ItemStack.EMPTY)) {
            caster.getInventory().placeItemBackInInventory(drop, Prediction.SERVER_ONLY);
        }
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(old));
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
        if (!caster.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.playSound(null, pos, replacement.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        return true;
    }
}
