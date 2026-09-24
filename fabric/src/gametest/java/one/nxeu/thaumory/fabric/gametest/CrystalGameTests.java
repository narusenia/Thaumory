package one.nxeu.thaumory.fabric.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.item.ThaumoryItems;

/** Arcane crystals in caves (requirements §17.3). */
public class CrystalGameTests {
    @GameTest(maxTicks = 20, padding = 2)
    public void crystalsGrowOnTheWallsOfACave(GameTestHelper helper) {
        // A stone box with a 5×5×5 hollow in the middle. How many crystals grow is random; each must sit on stone.
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 7, 7))) {
            helper.setBlock(pos, Blocks.STONE);
        }
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 2, 2), new BlockPos(6, 6, 6))) {
            helper.setBlock(pos, Blocks.AIR);
        }
        Feature feature = helper.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Thaumory.id("arcane_crystal"));
        helper.assertTrue(feature != null, "no arcane_crystal feature");
        helper.assertTrue(feature.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), helper.getLevel().getRandom(),
                helper.absolutePos(new BlockPos(4, 4, 4))), "feature did not place");
        int crystals = 0;
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(2, 2, 2), new BlockPos(6, 6, 6))) {
            BlockState state = helper.getBlockState(pos);
            if (state.is(ThaumoryBlocks.ARCANE_CRYSTAL.get())) {
                crystals++;
                BlockPos behind = pos.relative(state.getValue(AmethystClusterBlock.FACING).getOpposite());
                helper.assertBlockPresent(Blocks.STONE, behind);
            }
        }
        helper.assertTrue(crystals >= 1, "no crystals grew");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void aCrystalBrokenWithoutAPickaxeDropsOneShard(GameTestHelper helper) {
        BlockPos crystal = new BlockPos(3, 2, 3);
        helper.setBlock(crystal.below(), Blocks.STONE);
        helper.setBlock(crystal, ThaumoryBlocks.ARCANE_CRYSTAL.get());
        helper.getLevel().destroyBlock(helper.absolutePos(crystal), true);
        helper.succeedWhen(() -> helper.assertItemEntityCountIs(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get(), crystal, 2.0, 1));
    }
}
