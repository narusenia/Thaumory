package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Picks which animals the breeding circle pairs up (requirements §17.4): two of the same kind at a
 * time, never from a kind that already has {@code cap} or more in range. Each pair counts as one
 * more of its kind, since it will have a young one.
 */
public final class BreedingPairs {
    private BreedingPairs() {}

    /**
     * @param ready      the animals that can fall in love now, by kind
     * @param population how many of each kind are in range, young ones included
     * @param nextInt    a random index below its argument
     * @return up to {@code pairs} pairs, each two animals of one kind, none used twice
     */
    public static <K, T> List<List<T>> choose(Map<K, List<T>> ready, Map<K, Integer> population, int cap, int pairs,
            IntUnaryOperator nextInt) {
        Map<K, List<T>> left = new HashMap<>();
        ready.forEach((kind, animals) -> left.put(kind, new ArrayList<>(animals)));
        Map<K, Integer> counts = new HashMap<>(population);
        List<List<T>> chosen = new ArrayList<>();
        while (chosen.size() < pairs) {
            List<K> open = new ArrayList<>();
            for (Map.Entry<K, List<T>> entry : left.entrySet()) {
                if (entry.getValue().size() >= 2 && counts.getOrDefault(entry.getKey(), 0) < cap) {
                    open.add(entry.getKey());
                }
            }
            if (open.isEmpty()) {
                break;
            }
            K kind = open.get(nextInt.applyAsInt(open.size()));
            List<T> animals = left.get(kind);
            T first = animals.remove(nextInt.applyAsInt(animals.size()));
            T second = animals.remove(nextInt.applyAsInt(animals.size()));
            chosen.add(List.of(first, second));
            counts.merge(kind, 1, Integer::sum);
        }
        return chosen;
    }
}
