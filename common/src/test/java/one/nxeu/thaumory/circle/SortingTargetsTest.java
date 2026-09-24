package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SortingTargetsTest {
    @Test
    void aContainerWithTheSameItemComesFirstEvenIfFarther() {
        List<String> order = SortingTargets.order(List.of(
                new SortingTargets.Candidate<>("near", false, 1),
                new SortingTargets.Candidate<>("same", true, 25),
                new SortingTargets.Candidate<>("middle", false, 9)));
        assertEquals(List.of("same", "near", "middle"), order);
    }

    @Test
    void nearestFirstAmongThoseHoldingTheItem() {
        List<String> order = SortingTargets.order(List.of(
                new SortingTargets.Candidate<>("far", true, 16),
                new SortingTargets.Candidate<>("close", true, 4)));
        assertEquals(List.of("close", "far"), order);
    }
}
