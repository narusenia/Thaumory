package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BreedingPairsTest {
    private static int first(int bound) {
        return 0;
    }

    @Test
    void pairsTwoOfTheSameKind() {
        List<List<String>> pairs = BreedingPairs.choose(Map.of("cow", List.of("c1", "c2"), "pig", List.of("p1")),
                Map.of("cow", 2, "pig", 1), 16, 3, BreedingPairsTest::first);
        assertEquals(List.of(List.of("c1", "c2")), pairs);
    }

    @Test
    void usesEachAnimalOnce() {
        List<List<String>> pairs = BreedingPairs.choose(Map.of("cow", List.of("c1", "c2", "c3", "c4", "c5")),
                Map.of("cow", 5), 16, 5, BreedingPairsTest::first);
        assertEquals(2, pairs.size());
        Set<String> used = new HashSet<>();
        pairs.forEach(used::addAll);
        assertEquals(4, used.size());
    }

    @Test
    void leavesAFullKindAlone() {
        assertTrue(BreedingPairs.choose(Map.of("cow", List.of("c1", "c2")), Map.of("cow", 16), 16, 1, BreedingPairsTest::first)
                .isEmpty());
    }

    @Test
    void countsTheYoungOnesOnTheWay() {
        List<List<String>> pairs = BreedingPairs.choose(Map.of("cow", List.of("c1", "c2", "c3", "c4")), Map.of("cow", 15), 16, 2,
                BreedingPairsTest::first);
        assertEquals(1, pairs.size());
    }
}
