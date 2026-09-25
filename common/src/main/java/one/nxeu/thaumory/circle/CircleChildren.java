package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.List;

/**
 * Which Cores on a circle's nodes run as its sub-circles (requirements §4.6). Only a Core of lower
 * rank than the circle's sits in it at all; one of equal or higher rank is just part of the ring.
 * Of those that sit, the ones on the outermost ring that holds are children, up to the parent's
 * rank − 1, north, east, south and west in turn. The rest sit but do nothing.
 */
public final class CircleChildren {
    private CircleChildren() {}

    /** How a Core sits on a node of a circle of higher rank. */
    public enum Seat {
        /** Runs as a sub-circle. */
        CHILD,
        /** On a ring inside the outermost one that holds. */
        INNER_RING,
        /** On the outermost ring, past as many children as the parent can hold. */
        TOO_MANY
    }

    /** A Core on a node, and its rank. */
    public record Candidate(CircleScan.Node node, int rank) {}

    public record Placed(CircleScan.Node node, Seat seat) {}

    /** How many sub-circles a circle of rank {@code rank} holds: none at rank 1, one more each rank. */
    public static int limit(int rank) {
        return Math.max(0, rank - 1);
    }

    /**
     * @param candidates the Cores on the nodes of the rings that hold, inner ring first and in side
     *                   order within a ring, as {@link CircleScan#seats} lists them
     * @param rings      how many rings hold
     * @param rank       the parent's rank
     * @return the candidates that sit, in the same order
     */
    public static List<Placed> place(List<Candidate> candidates, int rings, int rank) {
        List<Placed> placed = new ArrayList<>();
        int children = 0;
        for (Candidate candidate : candidates) {
            if (candidate.rank() >= rank) {
                continue;
            }
            Seat seat;
            if (candidate.node().ring() != rings) {
                seat = Seat.INNER_RING;
            } else if (children < limit(rank)) {
                seat = Seat.CHILD;
                children++;
            } else {
                seat = Seat.TOO_MANY;
            }
            placed.add(new Placed(candidate.node(), seat));
        }
        return List.copyOf(placed);
    }

    /** How many of {@code placed} run as children. */
    public static int children(List<Placed> placed) {
        return (int) placed.stream().filter(p -> p.seat() == Seat.CHILD).count();
    }
}
