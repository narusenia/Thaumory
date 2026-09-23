package one.nxeu.thaumory.aspect.estimate;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BESTIA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.METALLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.MORS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ORDO;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VITA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;
import org.junit.jupiter.api.Test;

class AspectEstimatorTest {
    private final Map<Identifier, AspectList> manual = new HashMap<>();
    private final Map<Identifier, Identifier> remainders = new HashMap<>();

    @Test
    void dividesByCount() {
        manual.put(id("log"), AspectList.of(HERBA, 16));
        assertEstimated("planks", aspects(HERBA, 4), recipe("planks_from_log", "planks", 4, slot("log")));
    }

    @Test
    void roundsDownOnceAtTheEnd() {
        manual.put(id("spark"), AspectList.of(IGNIS, 1));
        // Rounding each third of a spark would give 0; rounding the sum gives exactly 1.
        assertEstimated("flame", aspects(IGNIS, 1),
                recipe("flame", "flame", 3, slot("spark"), slot("spark"), slot("spark")));
    }

    @Test
    void addsBonus() {
        manual.put(id("raw_iron"), AspectList.of(METALLUM, 16));
        EstimationRecipe smelting = new EstimationRecipe(
                id("smelt_iron"), List.of(slot("raw_iron")), id("iron_ingot"), 1, AspectList.of(IGNIS, 1));
        assertEstimated("iron_ingot", AspectList.of(new AspectStack(METALLUM, 16), new AspectStack(IGNIS, 1)), smelting);
    }

    @Test
    void worldChangesAddWhatTheWorldContributed() {
        manual.put(id("copper_block"), AspectList.of(METALLUM, 20));
        EstimationRecipe oxidizing = EstimationRecipe.worldChange(
                id("oxidize"), id("copper_block"), id("exposed_copper"), AspectList.of(AQUA, 1));
        assertEstimated("exposed_copper", AspectList.of(new AspectStack(METALLUM, 20), new AspectStack(AQUA, 1)), oxidizing);
    }

    @Test
    void usesCheapestItemInSlotAndBreaksTiesById() {
        manual.put(id("birch_planks"), AspectList.of(HERBA, 8));
        manual.put(id("oak_planks"), AspectList.of(HERBA, 4));
        manual.put(id("acacia_planks"), AspectList.of(TERRA, 4));
        // oak and acacia tie on total; acacia sorts first.
        assertEstimated("stick", aspects(TERRA, 4),
                recipe("stick", "stick", 1, slot("birch_planks", "oak_planks", "acacia_planks")));
    }

    @Test
    void picksCheapestRecipeAndBreaksTiesByRecipeId() {
        manual.put(id("gold"), AspectList.of(METALLUM, 8));
        manual.put(id("clay"), AspectList.of(TERRA, 4));
        manual.put(id("sand"), AspectList.of(AQUA, 4));

        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("b_from_clay", "brick", 1, slot("clay")),
                recipe("c_from_gold", "brick", 1, slot("gold")),
                recipe("a_from_sand", "brick", 1, slot("sand"))), this::remainder);

        assertEquals(aspects(AQUA, 4), result.estimated().get(id("brick")));
    }

    @Test
    void packingAndUnpackingNeitherLosesNorGains() {
        manual.put(id("raw_iron"), AspectList.of(METALLUM, 16));
        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("ingot_from_raw", "iron_ingot", 1, slot("raw_iron")),
                recipe("block_from_ingots", "iron_block", 1, slots("iron_ingot", 9)),
                recipe("ingots_from_block", "iron_ingot", 9, slot("iron_block"))), this::remainder);

        assertEquals(aspects(METALLUM, 16), result.estimated().get(id("iron_ingot")));
        assertEquals(aspects(METALLUM, 144), result.estimated().get(id("iron_block")));
        assertEquals(2, result.rounds());
    }

    @Test
    void cheaperRouteFoundInLaterRoundLowersValueAndDependents() {
        manual.put(id("bamboo"), AspectList.of(HERBA, 8));
        manual.put(id("log"), AspectList.of(HERBA, 16));
        manual.put(id("coal"), AspectList.of(IGNIS, 16));

        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("planks", "planks", 4, slot("log")),
                recipe("stick_from_bamboo", "stick", 1, slot("bamboo"), slot("bamboo")),
                recipe("stick_from_planks", "stick", 4, slot("planks"), slot("planks")),
                recipe("torch", "torch", 4, slot("coal"), slot("stick"))), this::remainder);

        // Bamboo settles sticks at Herba 16 in round one; planks (Herba 4) arrive in round two
        // and bring sticks down to 8 / 4 = 2.
        assertEquals(aspects(HERBA, 2), result.estimated().get(id("stick")));
        // The torch follows: (16 Ignis + 2 Herba) / 4 = 4 Ignis, 0.5 Herba.
        assertEquals(aspects(IGNIS, 4), result.estimated().get(id("torch")));
        assertTrue(result.fell().containsKey(id("stick")));
    }

    @Test
    void recoloringNeverGains() {
        manual.put(id("string"), AspectList.of(BESTIA, 8));
        manual.put(id("white_dye"), AspectList.of(LUX, 4));
        manual.put(id("red_dye"), AspectList.of(IGNIS, 4));

        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("white_wool", "white_wool", 1, slots("string", 4)),
                recipe("dye_white_wool", "white_wool", 1, slot("red_wool"), slot("white_dye")),
                recipe("dye_red_wool", "red_wool", 1, slot("white_wool"), slot("red_dye"))), this::remainder);

        // White comes from string; red is white plus red dye. Dyeing red back to white costs more.
        assertEquals(32, result.estimated().get(id("white_wool")).total());
        assertEquals(36, result.estimated().get(id("red_wool")).total());
    }

    @Test
    void keepsFractionsUntilTheEnd() {
        manual.put(id("raw_iron"), AspectList.of(METALLUM, 16));
        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("ingot_from_raw", "iron_ingot", 1, slot("raw_iron")),
                recipe("nuggets", "iron_nugget", 9, slot("iron_ingot")),
                recipe("ingot_from_nuggets", "iron_ingot", 1, slots("iron_nugget", 9))), this::remainder);

        // A nugget is 16/9 = 1.77; rounding it to 1 early would sink the ingot to 9.
        assertEquals(aspects(METALLUM, 16), result.estimated().get(id("iron_ingot")));
        assertEquals(aspects(METALLUM, 1), result.estimated().get(id("iron_nugget")));
    }

    @Test
    void shapeLoopsKeepTheirValue() {
        manual.put(id("quartz"), AspectList.of(ORDO, 16));
        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("block", "quartz_block", 1, slots("quartz", 4)),
                // Slabs take any quartz block, including the chiseled one made from slabs.
                recipe("slab", "quartz_slab", 6, slotsOfAny(3, "quartz_block", "chiseled_quartz_block")),
                recipe("chiseled", "chiseled_quartz_block", 1, slots("quartz_slab", 2))), this::remainder);

        assertEquals(aspects(ORDO, 32), result.estimated().get(id("quartz_slab")));
        assertEquals(aspects(ORDO, 64), result.estimated().get(id("chiseled_quartz_block")));
    }

    @Test
    void loopsWithoutOutsideInputStayUnresolved() {
        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("a_from_b", "a", 1, slot("b")),
                recipe("b_from_a", "b", 1, slot("a"))), this::remainder);

        assertEquals(Map.of(), result.estimated());
        assertEquals(Set.of(id("a"), id("b")), result.unresolved());
    }

    @Test
    void subtractsRemainder() {
        manual.put(id("bucket"), AspectList.of(METALLUM, 8));
        manual.put(id("milk_bucket"), AspectList.of(new AspectStack(METALLUM, 8), new AspectStack(BESTIA, 4)));
        manual.put(id("wheat"), AspectList.of(HERBA, 4));
        remainders.put(id("milk_bucket"), id("bucket"));

        // No Metallum leaks into the cake.
        assertEstimated("cake", AspectList.of(new AspectStack(BESTIA, 4), new AspectStack(HERBA, 4)),
                recipe("cake", "cake", 1, slot("milk_bucket"), slot("wheat")));
    }

    @Test
    void waitsForRemainderToSettle() {
        manual.put(id("iron"), AspectList.of(METALLUM, 3));
        manual.put(id("water_bucket"), AspectList.of(new AspectStack(METALLUM, 3), new AspectStack(AQUA, 4)));
        remainders.put(id("water_bucket"), id("bucket"));

        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("bucket", "bucket", 1, slot("iron")),
                recipe("mud", "mud", 1, slot("water_bucket"))), this::remainder);

        // bucket settles in round 1 (Metallum 3), so mud settles in round 2 with just the water.
        assertEquals(aspects(AQUA, 4), result.estimated().get(id("mud")));
        assertEquals(2, result.rounds());
    }

    @Test
    void handWrittenValuesWinIncludingExplicitlyNone() {
        manual.put(id("log"), AspectList.of(HERBA, 8));
        manual.put(id("barrier"), AspectList.empty());

        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("barrier", "barrier", 1, slot("log"))), this::remainder);

        assertFalse(result.estimated().containsKey(id("barrier")));
    }

    @Test
    void keepsSixLargestAspects() {
        manual.put(id("a"), AspectList.of(
                new AspectStack(IGNIS, 40), new AspectStack(AER, 36), new AspectStack(VITA, 32),
                new AspectStack(AQUA, 28), new AspectStack(TERRA, 24), new AspectStack(MORS, 20)));
        manual.put(id("b"), AspectList.of(HERBA, 8));

        AspectList result = AspectEstimator.estimate(manual, List.of(recipe("c", "c", 1, slot("a"), slot("b"))),
                this::remainder).estimated().get(id("c"));

        assertEquals(6, result.size());
        assertFalse(result.contains(HERBA));
    }

    private void assertEstimated(String item, AspectList expected, EstimationRecipe recipe) {
        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(recipe), this::remainder);
        assertEquals(expected, result.estimated().get(id(item)));
    }

    private Optional<Identifier> remainder(Identifier item) {
        return Optional.ofNullable(remainders.get(item));
    }

    @SafeVarargs
    private static EstimationRecipe recipe(String id, String result, int count, List<Identifier>... slots) {
        return recipe(id, result, count, List.of(slots));
    }

    private static EstimationRecipe recipe(String id, String result, int count, List<List<Identifier>> slots) {
        return new EstimationRecipe(id(id), slots, id(result), count, AspectList.empty());
    }

    private static List<Identifier> slot(String... items) {
        return Arrays.stream(items).map(AspectEstimatorTest::id).toList();
    }

    /** {@code count} identical slots. */
    private static List<List<Identifier>> slots(String item, int count) {
        return Collections.nCopies(count, slot(item));
    }

    /** {@code count} slots that each accept any of {@code items}. */
    private static List<List<Identifier>> slotsOfAny(int count, String... items) {
        return Collections.nCopies(count, slot(items));
    }

    private static AspectList aspects(Aspect aspect, int amount) {
        return AspectList.of(aspect, amount);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
