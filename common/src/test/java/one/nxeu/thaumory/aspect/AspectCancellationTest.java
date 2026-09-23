package one.nxeu.thaumory.aspect;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BELLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AspectCancellationTest {
    private AspectRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
    }

    @Test
    void leavesListsWithoutOppositesAlone() {
        AspectList aspects = AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 4));
        AspectCancellation.Result result = AspectCancellation.step(aspects, registry, 1);

        assertSame(aspects, result.remaining());
        assertEquals(0, result.removed());
    }

    @Test
    void oppositePrimalsLoseTheAmountEach() {
        AspectCancellation.Result result = AspectCancellation.step(
                AspectList.of(new AspectStack(IGNIS, 10), new AspectStack(AQUA, 10)), registry, 3);

        assertEquals(AspectList.of(new AspectStack(IGNIS, 7), new AspectStack(AQUA, 7)), result.remaining());
        assertEquals(6, result.removed());
    }

    @Test
    void compoundsCancelThroughTheirPrimals() {
        // Herba is Vita + Aqua, Bellum is Mors + Ignis: both Vita/Mors and Aqua/Ignis oppose, but it is one pair.
        AspectCancellation.Result result = AspectCancellation.step(
                AspectList.of(new AspectStack(HERBA, 5), new AspectStack(BELLUM, 5)), registry, 1);

        assertEquals(AspectList.of(new AspectStack(HERBA, 4), new AspectStack(BELLUM, 4)), result.remaining());
        assertEquals(2, result.removed());
    }

    @Test
    void anAspectInSeveralPairsLosesForEach() {
        // Aqua opposes Ignis and Lux (Ignis + Aer); Aer is only in Lux and does not oppose anything here.
        AspectCancellation.Result result = AspectCancellation.step(
                AspectList.of(new AspectStack(AQUA, 5), new AspectStack(IGNIS, 5), new AspectStack(LUX, 5), new AspectStack(AER, 5)),
                registry, 1);

        assertEquals(AspectList.of(new AspectStack(AQUA, 3), new AspectStack(IGNIS, 4), new AspectStack(LUX, 4), new AspectStack(AER, 5)),
                result.remaining());
        assertEquals(4, result.removed());
    }

    @Test
    void neverTakesMoreThanThereIs() {
        AspectCancellation.Result result = AspectCancellation.step(
                AspectList.of(new AspectStack(IGNIS, 1), new AspectStack(AQUA, 1), new AspectStack(LUX, 3)), registry, 2);

        // Aqua pairs with Ignis first and runs out, so Lux has nothing left to cancel against.
        assertEquals(AspectList.of(LUX, 3), result.remaining());
        assertEquals(2, result.removed());
    }
}
