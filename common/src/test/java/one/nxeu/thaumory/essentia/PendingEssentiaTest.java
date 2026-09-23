package one.nxeu.thaumory.essentia;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.api.essentia.EssentiaContainer;
import org.junit.jupiter.api.Test;

class PendingEssentiaTest {
    /** Holds up to 10 in total and lets only Ignis out. */
    private static final class Box implements EssentiaContainer {
        AspectList held;

        Box(AspectList held) {
            this.held = held;
        }

        @Override
        public AspectList contents() {
            return held;
        }

        @Override
        public int space(AspectList contents, Aspect aspect) {
            return Math.max(0, 10 - contents.total());
        }

        @Override
        public boolean canExtract(Aspect aspect) {
            return aspect.equals(IGNIS);
        }

        @Override
        public void update(AspectList contents) {
            held = contents;
        }
    }

    @Test
    void insertsUpToWhatTheContainerHasRoomFor() {
        Box box = new Box(AspectList.of(IGNIS, 4));
        PendingEssentia pending = new PendingEssentia(box);

        assertEquals(5, pending.insert(AQUA, 5));
        assertEquals(1, pending.insert(AQUA, 5));
        assertEquals(0, pending.insert(IGNIS, 5));
        assertEquals(AspectList.of(new AspectStack(IGNIS, 4), new AspectStack(AQUA, 6)), pending.contents());
    }

    @Test
    void extractsOnlyWhatTheContainerLetsOut() {
        PendingEssentia pending = new PendingEssentia(new Box(AspectList.of(new AspectStack(IGNIS, 4), new AspectStack(AQUA, 3))));

        assertEquals(0, pending.extract(AQUA, 3));
        assertEquals(4, pending.extract(IGNIS, 10));
        assertEquals(0, pending.extract(IGNIS, 10));
        assertEquals(AspectList.of(AQUA, 3), pending.contents());
    }

    @Test
    void leavesTheContainerAloneUntilHandedBack() {
        Box box = new Box(AspectList.of(IGNIS, 4));
        PendingEssentia pending = new PendingEssentia(box);
        AspectList before = pending.contents();

        pending.extract(IGNIS, 2);
        assertEquals(AspectList.of(IGNIS, 4), box.contents());
        assertTrue(pending.changed());

        pending.reset(before);
        assertFalse(pending.changed());
    }

    @Test
    void ignoresNegativeAmounts() {
        PendingEssentia pending = new PendingEssentia(new Box(AspectList.of(IGNIS, 4)));

        assertEquals(0, pending.insert(AQUA, -3));
        assertEquals(0, pending.extract(IGNIS, -3));
    }
}
