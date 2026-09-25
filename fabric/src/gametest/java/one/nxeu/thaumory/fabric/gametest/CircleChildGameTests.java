package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.StartResult;
import one.nxeu.thaumory.circle.CircleChildren.Seat;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.circle.CircleSide;

/** Sub-circles (requirements §4.6): Cores on the nodes of a circle of higher rank. */
public class CircleChildGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    /** A rank 2 Core with no runes inside four rings: a frame for one sub-circle. */
    private static CircleCoreBlockEntity parent(GameTestHelper helper) {
        return Circles.build(helper, CORE, ThaumoryBlocks.ARCANE_IRON_CIRCLE_CORE.get(), 4);
    }

    /** {@code block} in place of the chalk on the node of {@code ring} on {@code side}, holding {@code runes} and full of their Essentia. */
    private static CircleCoreBlockEntity seat(GameTestHelper helper, int ring, CircleSide side, CircleCoreBlock block, Aspect... runes) {
        BlockPos pos = CORE.offset(side.nodeX(ring), 0, side.nodeZ(ring));
        helper.setBlock(pos, block);
        CircleCoreBlockEntity core = helper.getBlockEntity(pos, CircleCoreBlockEntity.class);
        AspectList.Builder essentia = AspectList.builder();
        for (Aspect rune : runes) {
            core.insert(rune.id());
            essentia.add(rune, CircleCoreBlockEntity.capacity());
        }
        core.setEssentia(essentia.build());
        return core;
    }

    private static CircleCoreBlockEntity healing(GameTestHelper helper, CircleSide side) {
        return seat(helper, 4, side, ThaumoryBlocks.CIRCLE_CORE.get(), ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
    }

    private static void rescan(CircleCoreBlockEntity... cores) {
        for (CircleCoreBlockEntity core : cores) {
            core.rescan();
        }
    }

    // Four rings and a pig 20 blocks out reach far past the test area.
    @GameTest(maxTicks = 60, padding = 24)
    public void aSubCircleWorksOnItsParentsRange(GameTestHelper helper) {
        CircleCoreBlockEntity parent = parent(helper);
        CircleCoreBlockEntity child = healing(helper, CircleSide.EAST);
        rescan(parent, child);
        helper.assertValueEqual(parent.scan().rings(), 4, "the Core on the node keeps the ring whole");
        helper.assertValueEqual(parent.children(), 1, "children");
        helper.assertValueEqual(child.seat(), Optional.of(Seat.CHILD), "seat");
        helper.assertValueEqual(child.scan().rings(), 0, "a sub-circle reads no chalk of its own");
        helper.assertValueEqual(parent.instability(), CircleCoreBlockEntity.settings().childInstability(), "instability the child adds");
        helper.assertValueEqual(child.instability(), parent.instability(), "the child's instability is its parent's");
        helper.assertValueEqual(child.multipliers().orElseThrow().strength(), 1 + CircleCoreBlockEntity.settings().rankStrength(),
                "the child is as strong as its rank 2 parent");
        // Within four rings' reach of the parent, but past it from the child seven blocks east.
        BlockPos far = CORE.offset(-20, 0, 0);
        helper.setBlock(far.below(), Blocks.STONE);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, far);
        pig.setHealth(1);
        helper.assertValueEqual(child.start(Optional.empty()), StartResult.STARTED, "start");
        helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() > 1, "the pig at the far side of the parent was not healed"));
    }

    @GameTest(padding = 12)
    public void aCorePlacedAfterItsParentScannedSitsAtOnce(GameTestHelper helper) {
        // The parent scanned while the node still held chalk; only the new Core scans.
        parent(helper);
        CircleCoreBlockEntity child = healing(helper, CircleSide.SOUTH);
        child.rescan();
        helper.assertValueEqual(child.seat(), Optional.of(Seat.CHILD), "seat");
        helper.assertValueEqual(child.start(Optional.empty()), StartResult.STARTED, "start");
        helper.succeed();
    }

    @GameTest(padding = 12)
    public void coresPastTheLimitOrInsideSitIdle(GameTestHelper helper) {
        CircleCoreBlockEntity parent = parent(helper);
        CircleCoreBlockEntity north = healing(helper, CircleSide.NORTH);
        CircleCoreBlockEntity south = healing(helper, CircleSide.SOUTH);
        CircleCoreBlockEntity inner = seat(helper, 2, CircleSide.EAST, ThaumoryBlocks.CIRCLE_CORE.get(), ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        rescan(parent, north, south, inner);
        helper.assertValueEqual(parent.scan().rings(), 4, "rings");
        helper.assertValueEqual(parent.children(), 1, "a rank 2 circle holds one child");
        helper.assertValueEqual(north.seat(), Optional.of(Seat.CHILD), "north seat");
        helper.assertValueEqual(south.seat(), Optional.of(Seat.TOO_MANY), "south seat");
        helper.assertValueEqual(inner.seat(), Optional.of(Seat.INNER_RING), "inner seat");
        helper.assertValueEqual(south.start(Optional.empty()), StartResult.NO_RINGS, "the extra Core starting");
        helper.assertValueEqual(inner.start(Optional.empty()), StartResult.NO_RINGS, "the inner Core starting");
        helper.succeed();
    }

    @GameTest(padding = 12)
    public void aCoreOfTheSameRankIsOnlyPartOfTheRing(GameTestHelper helper) {
        CircleCoreBlockEntity parent = parent(helper);
        CircleCoreBlockEntity other = seat(helper, 4, CircleSide.WEST, ThaumoryBlocks.ARCANE_IRON_CIRCLE_CORE.get());
        rescan(parent, other);
        helper.assertValueEqual(parent.scan().rings(), 4, "rings");
        helper.assertValueEqual(parent.children(), 0, "children");
        helper.assertValueEqual(other.seat(), Optional.empty(), "seat");
        helper.succeed();
    }

    @GameTest(padding = 12)
    public void aSubCircleStopsWhenItsParentsRingBreaks(GameTestHelper helper) {
        CircleCoreBlockEntity parent = parent(helper);
        CircleCoreBlockEntity child = healing(helper, CircleSide.EAST);
        rescan(parent, child);
        helper.assertValueEqual(child.start(Optional.empty()), StartResult.STARTED, "start");
        CircleScan.Offset cell = CircleScan.cells(4).getFirst();
        helper.setBlock(CORE.offset(cell.dx(), 0, cell.dz()), Blocks.AIR);
        rescan(parent, child);
        helper.assertFalse(child.isRunning(), "the sub-circle kept running");
        helper.assertValueEqual(child.seat(), Optional.empty(), "seat once the ring broke");
        helper.succeed();
    }

    @GameTest(maxTicks = 20, padding = 12)
    public void aRunningSubCircleKeepsRunningWhenBothLoadAgain(GameTestHelper helper) {
        CircleCoreBlockEntity parent = parent(helper);
        CircleCoreBlockEntity child = healing(helper, CircleSide.EAST);
        rescan(parent, child);
        helper.assertValueEqual(child.start(Optional.empty()), StartResult.STARTED, "start");
        // What a chunk load does: fresh Cores read back from what was saved, neither scanned yet.
        ServerLevel level = helper.getLevel();
        BlockPos parentPos = helper.absolutePos(CORE);
        level.setBlockEntity(reloaded(level, parentPos, parent));
        BlockPos childPos = child.getBlockPos();
        CircleCoreBlockEntity loaded = reloaded(level, childPos, child);
        CircleCoreBlockEntity.serverTick(level, childPos, level.getBlockState(childPos), loaded);
        helper.assertTrue(loaded.isRunning(), "the sub-circle stopped after loading");
        helper.succeed();
    }

    private static CircleCoreBlockEntity reloaded(ServerLevel level, BlockPos pos, CircleCoreBlockEntity core) {
        CompoundTag saved = core.saveWithFullMetadata(level.registryAccess());
        CircleCoreBlockEntity loaded = (CircleCoreBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
        loaded.setLevel(level);
        return loaded;
    }
}
