package one.nxeu.thaumory.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;

/** Core ranks (requirements §4.6): rings read, rune slots and strength. */
public class CircleRankGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private static void readsRings(GameTestHelper helper, CircleCoreBlock block, int expected) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, block, 5, ThaumoryAspects.LUX, ThaumoryAspects.IGNIS);
        helper.assertValueEqual(core.maxRings(), expected, "max rings");
        helper.assertValueEqual(core.scan().rings(), expected, "rings read");
        helper.succeed();
    }

    // Five rings (19 × 19) reach well past the test area, so each test keeps its neighbours at a distance.
    @GameTest(padding = 12)
    public void rankOneReadsThreeRings(GameTestHelper helper) {
        readsRings(helper, ThaumoryBlocks.CIRCLE_CORE.get(), 3);
    }

    @GameTest(padding = 12)
    public void rankTwoReadsFourRings(GameTestHelper helper) {
        readsRings(helper, ThaumoryBlocks.ARCANE_IRON_CIRCLE_CORE.get(), 4);
    }

    @GameTest(padding = 12)
    public void rankThreeReadsFiveRings(GameTestHelper helper) {
        readsRings(helper, ThaumoryBlocks.AETHER_SILVER_CIRCLE_CORE.get(), 5);
    }

    @GameTest
    public void onlyRankThreeTakesAFourthRune(GameTestHelper helper) {
        for (CircleCoreBlock block : new CircleCoreBlock[] {ThaumoryBlocks.CIRCLE_CORE.get(), ThaumoryBlocks.ARCANE_IRON_CIRCLE_CORE.get()}) {
            CircleCoreBlockEntity core = Circles.build(helper, CORE, block, 1, ThaumoryAspects.LUX, ThaumoryAspects.IGNIS, ThaumoryAspects.UMBRA);
            helper.assertFalse(core.insert(ThaumoryAspects.AER.id()), "rank " + core.rank() + " took a fourth rune");
            helper.assertValueEqual(core.runes().size(), 3, "runes in rank " + core.rank());
        }
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryBlocks.AETHER_SILVER_CIRCLE_CORE.get(), 1,
                ThaumoryAspects.LUX, ThaumoryAspects.IGNIS, ThaumoryAspects.UMBRA);
        helper.assertTrue(core.insert(ThaumoryAspects.AER.id()), "rank 3 refused a fourth rune");
        helper.assertValueEqual(core.runes().size(), 4, "runes in rank 3");
        helper.assertFalse(core.insert(ThaumoryAspects.TERRA.id()), "rank 3 took a fifth rune");
        helper.succeed();
    }

    @GameTest
    public void aHigherRankMakesTheCircleStronger(GameTestHelper helper) {
        CircleCoreBlockEntity low = Circles.build(helper, CORE, ThaumoryBlocks.CIRCLE_CORE.get(), 1, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        double lowStrength = low.multipliers().orElseThrow().strength();
        CircleCoreBlockEntity high = Circles.build(helper, CORE, ThaumoryBlocks.AETHER_SILVER_CIRCLE_CORE.get(), 1,
                ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        double highStrength = high.multipliers().orElseThrow().strength();
        helper.assertValueEqual(highStrength - lowStrength, 2 * CircleCoreBlockEntity.settings().rankStrength(), "strength gained by two ranks");
        helper.assertTrue(highStrength > lowStrength, "rank 3 is no stronger than rank 1");
        helper.succeed();
    }
}
