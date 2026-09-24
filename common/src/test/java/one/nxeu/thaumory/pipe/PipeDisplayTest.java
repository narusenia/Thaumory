package one.nxeu.thaumory.pipe;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectColors;
import org.junit.jupiter.api.Test;

class PipeDisplayTest {
    @Test
    void emptyShowsNothing() {
        assertEquals(PipeDisplay.EMPTY, PipeDisplay.of(AspectList.empty(), 16));
    }

    @Test
    void roundsToFourStepsButNeverHidesEssentia() {
        assertEquals(1, PipeDisplay.of(AspectList.of(IGNIS, 1), 64).level());
        assertEquals(2, PipeDisplay.of(AspectList.of(IGNIS, 8), 16).level());
        assertEquals(4, PipeDisplay.of(AspectList.of(IGNIS, 16), 16).level());
        assertEquals(4, PipeDisplay.of(AspectList.of(IGNIS, 20), 16).level());
    }

    @Test
    void mixesColorsByAmount() {
        AspectList carried = AspectList.of(new AspectStack(IGNIS, 3), new AspectStack(AQUA, 1));
        assertEquals(AspectColors.mix(carried), PipeDisplay.of(carried, 16).color());
    }
}
