package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.InfuseResult;
import one.nxeu.thaumory.block.stone.BurntCircle;
import one.nxeu.thaumory.block.stone.CircleStoneBlockEntity;
import one.nxeu.thaumory.block.stone.CircleStoneBlockEntity.Status;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.circle.InfusionSettings;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.knowledge.CircleCombination;

/** Circle stones (requirements §10.3): burning a circle into one, and the stone at work on its own. */
public class CircleStoneGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);
    private static final BlockPos STONE = new BlockPos(4, 2, 4);

    /** Infuses a blank circle stone on a pedestal in a circle of {@code runes}, never failing. */
    private static CircleCoreBlockEntity infuse(GameTestHelper helper, InfuseResult expected, Aspect... runes) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, runes);
        helper.setBlock(CORE, helper.getBlockState(CORE).setValue(CircleCoreBlock.PEDESTAL, true));
        core.setPedestalItem(new ItemStack(ThaumoryItems.BLANK_CIRCLE_STONE.get()));
        CircleSettings current = CircleCoreBlockEntity.settings();
        InfusionSettings infusion = current.infusion();
        CircleCoreBlockEntity.updateSettings(new CircleSettings(current.scanInterval(), current.instabilityThreshold(), current.instabilityFlux(),
                current.undefinedFlux(), current.childInstability(), current.ringRadius(), current.rankStrength(), current.essentiaCapacity(),
                current.stoneRadius(), current.patterns(),
                new InfusionSettings(0, infusion.failurePerPoint(), infusion.fluxRatio(), infusion.maxLevel(), infusion.itemEssentia())));
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), expected, "result");
        } finally {
            CircleCoreBlockEntity.updateSettings(CircleSettings.DEFAULT);
        }
        return core;
    }

    /** A healing circle stone on the floor, holding {@code each} of Vita and Ordo. */
    private static CircleStoneBlockEntity healingStone(GameTestHelper helper, int each) {
        Circles.floor(helper);
        helper.setBlock(STONE, ThaumoryBlocks.CIRCLE_STONE.get());
        CircleStoneBlockEntity stone = helper.getBlockEntity(STONE, CircleStoneBlockEntity.class);
        ItemStack item = new ItemStack(ThaumoryItems.CIRCLE_STONE.get());
        item.set(ThaumoryComponents.BURNT_CIRCLE.get(), new BurntCircle(
                new CircleCombination(ThaumoryAspects.VITA.id(), ThaumoryAspects.ORDO.id(), Optional.empty()), ThaumoryCircleEffects.HEALING));
        stone.applyComponentsFromItemStack(item);
        if (each > 0) {
            stone.setEssentia(AspectList.builder().add(ThaumoryAspects.VITA, each).add(ThaumoryAspects.ORDO, each).build());
        }
        return stone;
    }

    @GameTest
    public void aBlankStoneTakesTheCircle(GameTestHelper helper) {
        CircleCoreBlockEntity core = infuse(helper, InfuseResult.INFUSED_STONE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        ItemStack stone = core.pedestalItem();
        helper.assertTrue(stone.is(ThaumoryItems.CIRCLE_STONE.get()), "the blank stone did not turn into a circle stone");
        helper.assertValueEqual(stone.get(ThaumoryComponents.BURNT_CIRCLE.get()), new BurntCircle(
                new CircleCombination(ThaumoryAspects.VITA.id(), ThaumoryAspects.ORDO.id(), Optional.empty()), ThaumoryCircleEffects.HEALING),
                "burnt circle");
        helper.assertTrue(core.essentia().amount(ThaumoryAspects.VITA) < CircleCoreBlockEntity.capacity(), "the infusion was free");
        helper.succeed();
    }

    @GameTest
    public void aStoneTakesEffectsNoItemDoes(GameTestHelper helper) {
        infuse(helper, InfuseResult.INFUSED_STONE, ThaumoryAspects.HERBA, ThaumoryAspects.MORS);
        helper.succeed();
    }

    @GameTest
    public void aStoneRefusesTriggeredAndChargingCircles(GameTestHelper helper) {
        CircleCoreBlockEntity teleport = infuse(helper, InfuseResult.NOT_FOR_STONE, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER,
                ThaumoryAspects.TERRA);
        helper.assertTrue(teleport.pedestalItem().is(ThaumoryItems.BLANK_CIRCLE_STONE.get()), "the refused stone changed");
        infuse(helper, InfuseResult.NOT_FOR_STONE, ThaumoryAspects.ARCANUM, ThaumoryAspects.VINCULUM);
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void aPlacedStoneHealsWithinItsRadius(GameTestHelper helper) {
        healingStone(helper, CircleStoneBlockEntity.capacity());
        Pig near = helper.spawnWithNoFreeWill(EntityTypes.PIG, STONE.offset(2, 0, 0));
        near.setHealth(1);
        // Past the radius of 4 (the box reaches 4 blocks from the stone's own block).
        BlockPos farPos = STONE.offset(-7, 0, 0);
        helper.setBlock(farPos.below(), Blocks.STONE);
        Pig far = helper.spawnWithNoFreeWill(EntityTypes.PIG, farPos);
        far.setHealth(1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(near.getHealth() > 1, "the pig near the stone was not healed"))
                .thenExecute(() -> helper.assertValueEqual(far.getHealth(), 1.0f, "the far pig's health"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 20)
    public void aSignalRestsTheStone(GameTestHelper helper) {
        CircleStoneBlockEntity stone = healingStone(helper, CircleStoneBlockEntity.capacity());
        helper.setBlock(STONE.east(), Blocks.REDSTONE_BLOCK);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(stone.status(), Status.POWERED, "status with a signal"))
                .thenExecute(() -> helper.setBlock(STONE.east(), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertValueEqual(stone.status(), Status.RUNNING, "status once the signal went"))
                .thenSucceed();
    }

    @GameTest(maxTicks = 20)
    public void aDryStoneStartsOnceFilled(GameTestHelper helper) {
        CircleStoneBlockEntity stone = healingStone(helper, 0);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(stone.status(), Status.NO_ESSENTIA, "status while dry"))
                .thenExecute(() -> stone.container().update(AspectList.builder().add(ThaumoryAspects.VITA, 4).add(ThaumoryAspects.ORDO, 4).build()))
                .thenWaitUntil(() -> helper.assertValueEqual(stone.status(), Status.RUNNING, "status once filled"))
                .thenExecute(() -> helper.assertValueEqual(stone.essentia().amount(ThaumoryAspects.VITA), 3, "Vita left after the first payment"))
                .thenSucceed();
    }

    /** Far from other tests: the Essentia left in it turns to Flux in its chunk. */
    @GameTest(padding = 24)
    public void aBrokenStoneDropsItsCircleAndLeaksItsEssentia(GameTestHelper helper) {
        CircleStoneBlockEntity stone = healingStone(helper, 10);
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(STONE));
        double before = ThaumoryApi.flux().get(helper.getLevel(), chunk);
        BurntCircle burnt = stone.burnt().orElseThrow();
        helper.getLevel().destroyBlock(helper.absolutePos(STONE), true);
        ItemEntity dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(STONE)).inflate(2)).stream()
                .filter(item -> item.getItem().is(ThaumoryItems.CIRCLE_STONE.get())).findFirst().orElse(null);
        helper.assertTrue(dropped != null, "no circle stone dropped");
        helper.assertValueEqual(dropped.getItem().get(ThaumoryComponents.BURNT_CIRCLE.get()), burnt, "the dropped stone's circle");
        helper.assertTrue(ThaumoryApi.flux().get(helper.getLevel(), chunk) >= before + 19, "the 20 Essentia inside did not turn to Flux");
        helper.succeed();
    }
}
