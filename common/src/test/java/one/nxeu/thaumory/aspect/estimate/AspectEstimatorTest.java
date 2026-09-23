package one.nxeu.thaumory.aspect.estimate;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BESTIA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.METALLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.MORS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VITA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    void dividesByCountThenDecays() {
        manual.put(id("log"), AspectList.of(HERBA, 8));
        // 8 * 3/4 / 2 = 3
        assertEstimated("planks", aspects(HERBA, 3), recipe("planks_from_log", "planks", 2, slot("log")));
    }

    @Test
    void roundsDownOnceAtTheEnd() {
        manual.put(id("spark"), AspectList.of(IGNIS, 1));
        // Per-slot rounding would give 0 + 0; rounding the sum gives floor(2 * 3/4) = 1.
        assertEstimated("flame", aspects(IGNIS, 1), recipe("flame", "flame", 1, slot("spark"), slot("spark")));
    }

    @Test
    void addsBonusAfterDecay() {
        manual.put(id("raw_iron"), AspectList.of(METALLUM, 4));
        EstimationRecipe smelting = new EstimationRecipe(
                id("smelt_iron"), List.of(slot("raw_iron")), id("iron_ingot"), 1, AspectList.of(IGNIS, 1));
        assertEstimated("iron_ingot", AspectList.of(new AspectStack(METALLUM, 3), new AspectStack(IGNIS, 1)), smelting);
    }

    @Test
    void usesCheapestItemInSlotAndBreaksTiesById() {
        manual.put(id("birch_planks"), AspectList.of(HERBA, 8));
        manual.put(id("oak_planks"), AspectList.of(HERBA, 4));
        manual.put(id("acacia_planks"), AspectList.of(TERRA, 4));
        // oak and acacia tie on total; acacia sorts first.
        assertEstimated("stick", aspects(TERRA, 3),
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

        assertEquals(aspects(AQUA, 3), result.estimated().get(id("brick")));
    }

    @Test
    void settledValuesIgnoreLaterCheaperLoops() {
        manual.put(id("raw_iron"), AspectList.of(METALLUM, 16));
        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("ingot_from_raw", "iron_ingot", 1, slot("raw_iron")),
                recipe("block_from_ingots", "iron_block", 1, slots("iron_ingot", 9)),
                recipe("ingots_from_block", "iron_ingot", 9, slot("iron_block"))), this::remainder);

        assertEquals(aspects(METALLUM, 12), result.estimated().get(id("iron_ingot")));
        assertEquals(aspects(METALLUM, 81), result.estimated().get(id("iron_block")));
        assertEquals(2, result.rounds());
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

        // (bestia 4) + (herba 4), * 3/4 -> 3 + 3; no Metallum leaks into the cake.
        assertEstimated("cake", AspectList.of(new AspectStack(BESTIA, 3), new AspectStack(HERBA, 3)),
                recipe("cake", "cake", 1, slot("milk_bucket"), slot("wheat")));
    }

    @Test
    void waitsForRemainderToSettle() {
        manual.put(id("iron"), AspectList.of(METALLUM, 4));
        manual.put(id("water_bucket"), AspectList.of(new AspectStack(METALLUM, 3), new AspectStack(AQUA, 4)));
        remainders.put(id("water_bucket"), id("bucket"));

        AspectEstimator.Result result = AspectEstimator.estimate(manual, List.of(
                recipe("bucket", "bucket", 1, slot("iron")),
                recipe("mud", "mud", 1, slot("water_bucket"))), this::remainder);

        // bucket settles in round 1 (Metallum 3), so mud settles in round 2 with just the water.
        assertEquals(aspects(AQUA, 3), result.estimated().get(id("mud")));
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

    private static AspectList aspects(Aspect aspect, int amount) {
        return AspectList.of(aspect, amount);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
