package one.nxeu.thaumory.circle.effect;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Herba + Mors, sustained. Sweeps the range for ripe crops and reaps them, dropping the harvest
 * where they stood (requirements §17.4). Field crops, nether wart and cocoa are planted again from
 * a seed out of the harvest; melons and pumpkins are cut from their stems; sugar cane, cactus and
 * bamboo are cut above the bottom block; sweet berries are picked.
 */
final class HarvestEffect implements CircleEffect {
    @Override
    public void apply(CircleContext context) {
        ServerLevel level = context.level();
        CircleRange.sweep(context, pos -> {
            if (harvest(level, pos, level.getBlockState(pos))) {
                context.affected(pos);
            }
        });
    }

    private static boolean harvest(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state) && replant(level, pos, state, crop.getStateForAge(0));
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE
                    && replant(level, pos, state, state.setValue(NetherWartBlock.AGE, 0));
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE && replant(level, pos, state, state.setValue(CocoaBlock.AGE, 0));
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) > 1 && pick(level, pos, state);
        }
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            return onAStem(level, pos) && level.destroyBlock(pos, true);
        }
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock) {
            // The second block from the bottom: cutting it brings down everything above.
            return level.getBlockState(pos.below()).is(block) && !level.getBlockState(pos.below(2)).is(block) && level.destroyBlock(pos, true);
        }
        return false;
    }

    /** Reaps a ripe plant and sets {@code young} in its place, paid for with one seed from the harvest. */
    private static boolean replant(ServerLevel level, BlockPos pos, BlockState ripe, BlockState young) {
        List<ItemStack> drops = Block.getDrops(ripe, level, pos, null);
        boolean seeded = takeSeed(drops, ripe.getBlock().asItem());
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(ripe));
        level.setBlock(pos, seeded ? young : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }
        return true;
    }

    private static boolean takeSeed(List<ItemStack> drops, Item seed) {
        for (ItemStack drop : drops) {
            if (drop.is(seed)) {
                drop.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Picks the berries off a bush the way a player would, leaving it bare. */
    private static boolean pick(ServerLevel level, BlockPos pos, BlockState state) {
        Block.dropFromBlockInteractLootTable(level, BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH, pos, state, null, null, null,
                (server, stack) -> Block.popResource(server, pos, stack));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
        level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), Block.UPDATE_CLIENTS);
        return true;
    }

    /** Whether a grown stem beside the block points at it, so it grew there and was not placed. */
    private static boolean onAStem(ServerLevel level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockState beside = level.getBlockState(pos.relative(side));
            if (beside.getBlock() instanceof AttachedStemBlock && beside.getValue(AttachedStemBlock.FACING) == side.getOpposite()) {
                return true;
            }
        }
        return false;
    }
}
