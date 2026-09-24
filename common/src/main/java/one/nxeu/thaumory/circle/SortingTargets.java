package one.nxeu.thaumory.circle;

import java.util.Comparator;
import java.util.List;

/**
 * The order the sorting circle tries containers in for one item (requirements §17.4): those that
 * already hold the same item first, then the rest; nearest to the Core first within each.
 */
public final class SortingTargets {
    private SortingTargets() {}

    /**
     * @param holdsSame      whether it already has the item being put away
     * @param distanceSquared how far it is from the Core, squared
     */
    public record Candidate<T>(T container, boolean holdsSame, double distanceSquared) {}

    public static <T> List<T> order(List<Candidate<T>> candidates) {
        return candidates.stream()
                .sorted(Comparator.<Candidate<T>, Boolean>comparing(c -> !c.holdsSame()).thenComparingDouble(Candidate::distanceSquared))
                .map(Candidate::container)
                .toList();
    }
}
