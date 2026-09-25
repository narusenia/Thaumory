package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.circle.CircleChildren.Candidate;
import one.nxeu.thaumory.circle.CircleChildren.Placed;
import one.nxeu.thaumory.circle.CircleChildren.Seat;
import org.junit.jupiter.api.Test;

class CircleChildrenTest {
    private static final Identifier CORE = Identifier.fromNamespaceAndPath("thaumory", "circle_core");

    private static Candidate at(int ring, CircleSide side, int rank) {
        return new Candidate(new CircleScan.Node(ring, side, CORE), rank);
    }

    private static List<Seat> seats(List<Placed> placed) {
        return placed.stream().map(Placed::seat).toList();
    }

    @Test
    void eachRankHoldsOneMoreThanTheLast() {
        assertEquals(0, CircleChildren.limit(1));
        assertEquals(1, CircleChildren.limit(2));
        assertEquals(2, CircleChildren.limit(3));
        assertEquals(4, CircleChildren.limit(5));
    }

    @Test
    void coresOnTheOutermostRingAreChildrenUpToTheLimit() {
        List<Placed> placed = CircleChildren.place(List.of(at(5, CircleSide.NORTH, 1), at(5, CircleSide.EAST, 2), at(5, CircleSide.WEST, 1)), 5, 3);

        assertEquals(List.of(Seat.CHILD, Seat.CHILD, Seat.TOO_MANY), seats(placed));
        assertEquals(2, CircleChildren.children(placed));
    }

    @Test
    void coresOnInnerRingsSitButDoNotRun() {
        List<Placed> placed = CircleChildren.place(List.of(at(2, CircleSide.SOUTH, 1), at(4, CircleSide.SOUTH, 1)), 4, 2);

        assertEquals(List.of(Seat.INNER_RING, Seat.CHILD), seats(placed));
    }

    @Test
    void coresOfEqualOrHigherRankDoNotSit() {
        List<Placed> placed = CircleChildren.place(List.of(at(4, CircleSide.NORTH, 2), at(4, CircleSide.EAST, 3), at(4, CircleSide.SOUTH, 1)), 4, 2);

        assertEquals(List.of(new Placed(new CircleScan.Node(4, CircleSide.SOUTH, CORE), Seat.CHILD)), placed);
    }

    @Test
    void rankOneHoldsNone() {
        assertEquals(List.of(), CircleChildren.place(List.of(at(3, CircleSide.NORTH, 1)), 3, 1));
    }
}
