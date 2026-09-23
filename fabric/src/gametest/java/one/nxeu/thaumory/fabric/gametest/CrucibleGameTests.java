package one.nxeu.thaumory.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;

/** The Crucible in the world: heat, water and opposites wearing each other down (requirements §3). */
public class CrucibleGameTests {
    private static final BlockPos CRUCIBLE = new BlockPos(2, 2, 2);

    /** Ignis 10 and Aqua 4 lose 1 each per second while boiling, until the Aqua is gone: 8 Flux. */
    @GameTest(maxTicks = 300)
    public void oppositesCancelIntoFlux(GameTestHelper helper) {
        helper.setBlock(CRUCIBLE.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(CRUCIBLE, ThaumoryBlocks.CRUCIBLE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(CRUCIBLE, player);

        CrucibleBlockEntity crucible = helper.getBlockEntity(CRUCIBLE, CrucibleBlockEntity.class);
        helper.assertTrue(crucible.tank().hasWater(), "the bucket did not fill the Crucible");
        crucible.setContents(AspectList.builder().add(ThaumoryAspects.IGNIS, 10).add(ThaumoryAspects.AQUA, 4).build());
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(CRUCIBLE));
        Thaumory.flux().set(helper.getLevel(), chunk, 0);

        helper.succeedWhen(() -> {
            AspectList contents = crucible.tank().contents();
            helper.assertValueEqual(contents.amount(ThaumoryAspects.AQUA), 0, "Aqua");
            helper.assertValueEqual(contents.amount(ThaumoryAspects.IGNIS), 6, "Ignis");
            // Flux decays slowly all the time, so a little under 8 is still 8 from cancelling.
            helper.assertValueInBetween(7.9, Thaumory.flux().get(helper.getLevel(), chunk), 8.0, "Flux");
        });
    }

    /** Without heat nothing boils, so nothing cancels. */
    @GameTest(maxTicks = 160)
    public void coldCrucibleKeepsOpposites(GameTestHelper helper) {
        helper.setBlock(CRUCIBLE.below(), Blocks.STONE);
        helper.setBlock(CRUCIBLE, ThaumoryBlocks.CRUCIBLE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(CRUCIBLE, player);
        CrucibleBlockEntity crucible = helper.getBlockEntity(CRUCIBLE, CrucibleBlockEntity.class);
        AspectList contents = AspectList.builder().add(ThaumoryAspects.IGNIS, 10).add(ThaumoryAspects.AQUA, 4).build();
        crucible.setContents(contents);
        helper.runAtTickTime(150, () -> {
            helper.assertValueEqual(crucible.tank().contents(), contents, "contents");
            helper.succeed();
        });
    }
}
