package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.block.Blocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.TriggerResult;
import one.nxeu.thaumory.circle.CirclePlane;
import one.nxeu.thaumory.item.ThaumoryItems;

/** Circles drawn on walls and ceilings (requirements §4.3). */
public class WallCircleGameTests {
    private static final BlockPos WALL_CORE = new BlockPos(4, 4, 4);

    @GameTest(maxTicks = 20)
    public void circlesHoldOnEveryFace(GameTestHelper helper) {
        for (Direction front : Direction.values()) {
            // Circles.build checks that the ring holds.
            CircleCoreBlockEntity core = Circles.build(helper, WALL_CORE, front, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
            helper.assertValueEqual(core.front(), front, "front");
            clear(helper, core);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void patternsOnAnotherFaceDoNotCount(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, WALL_CORE, Direction.NORTH, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        BlockPos corner = WALL_CORE.offset(CirclePlane.offset(Direction.NORTH, 1, 1));
        helper.setBlock(corner, ThaumoryBlocks.CHALK_LINE.get().defaultBlockState().setValue(ChalkPatternBlock.FACING, Direction.UP));
        core.rescan();
        helper.assertValueEqual(core.scan().rings(), 0, "rings");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void coreAndChalkFallOffWithoutSupport(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, WALL_CORE, Direction.NORTH, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        // Empty, so breaking it leaks no Flux into the tests around.
        core.setEssentia(AspectList.empty());
        BlockPos chalk = WALL_CORE.offset(CirclePlane.offset(Direction.NORTH, 1, 0));
        helper.setBlock(WALL_CORE.relative(Direction.SOUTH), Blocks.AIR);
        helper.setBlock(chalk.relative(Direction.SOUTH), Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, WALL_CORE);
            helper.assertBlockPresent(Blocks.AIR, chalk);
            helper.assertItemEntityPresent(ThaumoryItems.RUNE.get());
            helper.assertItemEntityPresent(ThaumoryItems.CIRCLE_CORE.get());
        });
    }

    // Each teleport test has its own channel, so it never finds the circles of the tests around it.
    @GameTest(maxTicks = 40)
    public void teleportLandsBelowAWallCircle(GameTestHelper helper) {
        Circles.floor(helper);
        BlockPos from = new BlockPos(1, 2, 1);
        BlockPos to = new BlockPos(5, 4, 5);
        CircleCoreBlockEntity here = Circles.build(helper, from, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER, ThaumoryAspects.IGNIS);
        Circles.build(helper, to, Direction.NORTH, ThaumoryAspects.AER, ThaumoryAspects.ARCANUM, ThaumoryAspects.IGNIS);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, from);
        helper.assertValueEqual(here.trigger(Optional.of(pig)), TriggerResult.TRIGGERED, "trigger");
        BlockPos footing = helper.absolutePos(new BlockPos(5, 2, 5));
        helper.succeedWhen(() -> helper.assertTrue(pig.blockPosition().equals(footing), "pig did not land below the wall circle"));
    }

    // The GameTest world outlives a run, so the circles of earlier runs are still about: this test
    // checks where the pig lands, which only the circles in its own area are near enough to decide.
    @GameTest(maxTicks = 40)
    public void teleportPassesOverAWallCircleWithNowhereToStand(GameTestHelper helper) {
        Circles.floor(helper);
        BlockPos from = new BlockPos(1, 2, 1);
        CircleCoreBlockEntity here = Circles.build(helper, from, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER, ThaumoryAspects.ORDO);
        // The nearer partner: bars under it block the way down but give no footing.
        Circles.build(helper, new BlockPos(5, 4, 2), Direction.NORTH, ThaumoryAspects.AER, ThaumoryAspects.ARCANUM, ThaumoryAspects.ORDO);
        helper.setBlock(new BlockPos(5, 2, 2), Blocks.IRON_BARS);
        Circles.build(helper, new BlockPos(5, 4, 6), Direction.NORTH, ThaumoryAspects.AER, ThaumoryAspects.ARCANUM, ThaumoryAspects.ORDO);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, from);
        helper.assertValueEqual(here.trigger(Optional.of(pig)), TriggerResult.TRIGGERED, "trigger");
        BlockPos footing = helper.absolutePos(new BlockPos(5, 2, 6));
        helper.succeedWhen(() -> helper.assertTrue(pig.blockPosition().equals(footing), "pig did not land at the farther circle"));
    }

    /** Clears the area for the next circle, emptying the Core first so it leaks no Flux. */
    private static void clear(GameTestHelper helper, CircleCoreBlockEntity core) {
        core.setEssentia(AspectList.empty());
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(7, 7, 7))) {
            helper.setBlock(pos, Blocks.AIR);
        }
        helper.killAllEntities();
    }
}
