package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.StartResult;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.TriggerResult;

/** The industrial circles: smelting, mining, sorting and melting (requirements §17.4). */
public class IndustryCircleGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private static void start(GameTestHelper helper, CircleCoreBlockEntity core) {
        helper.assertValueEqual(core.start(Optional.empty()), StartResult.STARTED, "start");
    }

    @GameTest(maxTicks = 200)
    public void smeltingCooksDroppedItemsOnce(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.IGNIS, ThaumoryAspects.METALLUM);
        helper.spawnItem(Items.RAW_IRON, new BlockPos(6, 2, 4));
        helper.spawnItem(Items.COBBLESTONE, new BlockPos(2, 2, 4));
        start(helper, core);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertItemEntityPresent(Items.IRON_INGOT);
                    helper.assertItemEntityPresent(Items.STONE);
                    helper.assertItemEntityNotPresent(Items.RAW_IRON);
                })
                .thenIdle(60)
                .thenExecute(() -> helper.assertItemEntityNotPresent(Items.SMOOTH_STONE))
                .thenSucceed();
    }

    // The layer reaches a block past the test area.
    @GameTest(maxTicks = 400, padding = 4)
    public void miningDigsOneLayerAtATimeBehindTheCircle(GameTestHelper helper) {
        BlockPos core = new BlockPos(4, 5, 4);
        for (int y = 2; y <= 3; y++) {
            for (int x = 2; x <= 6; x++) {
                for (int z = 2; z <= 6; z++) {
                    helper.setBlock(x, y, z, Blocks.STONE);
                }
            }
        }
        helper.setBlock(4, 3, 4, Blocks.IRON_ORE);
        helper.setBlock(3, 3, 3, Blocks.OBSIDIAN);
        helper.setBlock(5, 3, 5, Blocks.CHEST);
        CircleCoreBlockEntity entity = Circles.build(helper, core, ThaumoryAspects.BELLUM, ThaumoryAspects.TERRA);
        helper.assertValueEqual(entity.trigger(Optional.empty()), TriggerResult.TRIGGERED, "first trigger");
        int essentia = entity.essentia().amount(ThaumoryAspects.BELLUM);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 3, 4));
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 3, 6));
                    // The last block of the layer: dx runs outermost, dz within it.
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 3, 6));
                })
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(3, 3, 3));
                    helper.assertBlockPresent(Blocks.CHEST, new BlockPos(5, 3, 5));
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 4, 4));
                    helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 2, 4));
                    helper.assertItemEntityPresent(Items.RAW_IRON);
                    helper.assertItemEntityPresent(Items.COBBLESTONE);
                    // 23 blocks: the activation covers 8, then one each for the next two eights.
                    helper.assertValueEqual(entity.essentia().amount(ThaumoryAspects.BELLUM), essentia - 2, "paid while digging");
                })
                .thenWaitUntil(() -> helper.assertValueEqual(entity.trigger(Optional.empty()), TriggerResult.TRIGGERED, "second trigger"))
                .thenIdle(20)
                .thenExecute(() -> helper.assertValueEqual(entity.trigger(Optional.empty()), TriggerResult.STOPPED, "trigger while digging"))
                .thenIdle(40)
                .thenExecute(() -> helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 2, 6)))
                .thenExecute(() -> helper.assertValueEqual(entity.trigger(Optional.empty()), TriggerResult.TRIGGERED, "carry on"))
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.AIR, new BlockPos(6, 2, 6)))
                .thenSucceed();
    }

    @GameTest(maxTicks = 200, padding = 24)
    public void workingGivesOffFlux(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.IGNIS, ThaumoryAspects.METALLUM);
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(CORE));
        Thaumory.flux().set(helper.getLevel(), chunk, 5);
        helper.spawnItem(Items.RAW_IRON, new BlockPos(6, 2, 4));
        helper.spawnItem(Items.RAW_IRON, new BlockPos(6, 2, 4));
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertItemEntityPresent(Items.IRON_INGOT);
            helper.assertTrue(ThaumoryApi.flux().get(helper.getLevel(), chunk) > 5.1, "smelting gave off no Flux");
        });
    }

    @GameTest(maxTicks = 200)
    public void sortingPutsItemsWithTheirKindOrInTheNearestChest(GameTestHelper helper) {
        Circles.floor(helper);
        BlockPos holding = new BlockPos(1, 2, 1);
        BlockPos near = new BlockPos(6, 2, 4);
        helper.setBlock(holding, Blocks.CHEST);
        helper.setBlock(near, Blocks.CHEST);
        helper.getBlockEntity(holding, ChestBlockEntity.class).setItem(0, new ItemStack(Items.COBBLESTONE));
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ORDO, ThaumoryAspects.TEMPESTAS);
        helper.spawnItem(Items.COBBLESTONE, new BlockPos(4, 2, 7));
        helper.spawnItem(Items.DIRT, new BlockPos(4, 2, 7));
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertItemEntityNotPresent(Items.COBBLESTONE);
            helper.assertItemEntityNotPresent(Items.DIRT);
            helper.assertValueEqual(helper.getBlockEntity(holding, ChestBlockEntity.class).countItem(Items.COBBLESTONE), 2, "cobblestone with its kind");
            helper.assertValueEqual(helper.getBlockEntity(near, ChestBlockEntity.class).countItem(Items.DIRT), 1, "dirt in the nearest chest");
        });
    }

    @GameTest(maxTicks = 200, padding = 24)
    public void meltingKeepsSlotThreeAndTurnsTheRestToFlux(GameTestHelper helper) {
        Circles.floor(helper);
        AspectList log = ItemAspects.get(new ItemStack(Items.OAK_LOG));
        Aspect kept = log.largest().orElseThrow().aspect();
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.IGNIS, ThaumoryAspects.CHAOS, kept);
        core.setEssentia(AspectList.builder().add(ThaumoryAspects.IGNIS, 64).add(ThaumoryAspects.CHAOS, 64).build());
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(CORE));
        Thaumory.flux().set(helper.getLevel(), chunk, 0);
        helper.spawnItem(Items.OAK_LOG, new BlockPos(6, 2, 4));
        start(helper, core);
        boolean mixed = log.size() > 1;
        helper.succeedWhen(() -> {
            helper.assertItemEntityNotPresent(Items.OAK_LOG);
            helper.assertTrue(core.essentia().amount(kept) > 0, "nothing was kept");
            helper.assertTrue(!mixed || ThaumoryApi.flux().get(helper.getLevel(), chunk) > 0, "no Flux from the rest");
        });
    }
}
