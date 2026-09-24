package one.nxeu.thaumory.circle;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.Test;

class CircleMeltTest {
    private static final AspectList LOG = AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 4));

    @Test
    void keepsTheSlotThreeAspectAndTurnsTheRestToFlux() {
        assertEquals(new CircleMelt.Result(16, 4), CircleMelt.melt(LOG, 1.0, Optional.of(HERBA), 64));
    }

    @Test
    void withNothingKeptItIsAllFlux() {
        assertEquals(new CircleMelt.Result(0, 20), CircleMelt.melt(LOG, 1.0, Optional.empty(), 64));
    }

    @Test
    void whatDoesNotFitIsFluxToo() {
        assertEquals(new CircleMelt.Result(10, 10), CircleMelt.melt(LOG, 1.0, Optional.of(HERBA), 10));
    }

    @Test
    void takesTheCruciblesShareRoundedDown() {
        // 16 * 0.75 = 12 and 4 * 0.75 = 3.
        assertEquals(new CircleMelt.Result(12, 3), CircleMelt.melt(LOG, 0.75, Optional.of(HERBA), 64));
    }
}
