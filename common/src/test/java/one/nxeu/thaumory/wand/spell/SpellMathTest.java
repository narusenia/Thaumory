package one.nxeu.thaumory.wand.spell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import one.nxeu.thaumory.wand.spell.SpellMath.Point;
import org.junit.jupiter.api.Test;

class SpellMathTest {
    private static final Point EAST = new Point(1, 0, 0);

    @Test
    void theConeReachesAheadWithinItsAngle() {
        assertTrue(SpellMath.inCone(new Point(5, 0, 0), EAST, 6, 30));
        assertTrue(SpellMath.inCone(new Point(4, 0, 2), EAST, 6, 30));
        assertFalse(SpellMath.inCone(new Point(2, 0, 2), EAST, 6, 30));
        assertFalse(SpellMath.inCone(new Point(-3, 0, 0), EAST, 6, 30));
        assertFalse(SpellMath.inCone(new Point(7, 0, 0), EAST, 6, 30));
    }

    @Test
    void theLookNeedNotBeAUnitVector() {
        assertTrue(SpellMath.inCone(new Point(3, 1, 0), new Point(10, 0, 0), 6, 30));
    }

    @Test
    void lightningJumpsToTheNearestEachTime() {
        List<Point> candidates = List.of(new Point(7, 0, 0), new Point(3, 0, 0), new Point(0, 0, 3), new Point(20, 0, 0));
        // From the origin: 3,0,0 and 0,0,3 are equally near; the first found wins, then 7,0,0 is within 4 of it.
        assertEquals(List.of(1, 0), SpellMath.chain(new Point(0, 0, 0), candidates, 4, 2));
    }

    @Test
    void lightningStopsWhenNoOneIsInReach() {
        List<Point> candidates = List.of(new Point(3, 0, 0), new Point(20, 0, 0));
        assertEquals(List.of(0), SpellMath.chain(new Point(0, 0, 0), candidates, 4, 2));
        assertEquals(List.of(), SpellMath.chain(new Point(0, 0, 0), List.of(), 4, 2));
    }
}
