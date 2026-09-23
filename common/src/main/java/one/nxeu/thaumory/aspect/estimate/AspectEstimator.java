package one.nxeu.thaumory.aspect.estimate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;

/**
 * Estimates aspects for items without a datapack entry from their recipes (requirements §2.3).
 *
 * <p>Values are settled in rounds. Round one starts from the hand-written items. Each round, every
 * unsettled item that has a recipe whose inputs are all settled gets the value of its cheapest such
 * recipe, and all of them settle together at the end of the round. A settled value never changes,
 * so compress/decompress loops cannot drag values down. Items that never settle stay without
 * aspects.
 *
 * <p>One recipe is worth {@code floor(sum of inputs * 3 / (4 * count)) + bonus}, limited to the six
 * largest aspects. Each slot uses its cheapest settled item; an item that leaves a remainder (a
 * bucket from a milk bucket) counts as itself minus the remainder.
 */
public final class AspectEstimator {
    static final long DECAY_NUMERATOR = 3;
    static final long DECAY_DENOMINATOR = 4;
    static final int MAX_ASPECTS = 6;

    private static final Comparator<Identifier> BY_ID = Comparator.comparing(Identifier::toString);

    public record Result(Map<Identifier, AspectList> estimated, Set<Identifier> unresolved, int rounds) {}

    private AspectEstimator() {}

    /**
     * @param manual hand-written aspects; these items are never estimated
     * @param remainderOf the item left behind after crafting with an item, if any
     */
    public static Result estimate(
            Map<Identifier, AspectList> manual,
            Collection<EstimationRecipe> recipes,
            Function<Identifier, Optional<Identifier>> remainderOf) {
        Map<Identifier, List<EstimationRecipe>> byResult = new LinkedHashMap<>();
        recipes.stream()
                .filter(recipe -> !manual.containsKey(recipe.result()))
                .sorted(Comparator.comparing(EstimationRecipe::id, BY_ID))
                .forEach(recipe -> byResult.computeIfAbsent(recipe.result(), k -> new ArrayList<>()).add(recipe));

        Map<Identifier, AspectList> settled = new HashMap<>(manual);
        Map<Identifier, AspectList> estimated = new HashMap<>();
        int rounds = 0;
        while (true) {
            Map<Identifier, AspectList> settledThisRound = new HashMap<>();
            for (Map.Entry<Identifier, List<EstimationRecipe>> entry : byResult.entrySet()) {
                if (settled.containsKey(entry.getKey())) {
                    continue;
                }
                cheapest(entry.getValue(), settled, remainderOf)
                        .ifPresent(value -> settledThisRound.put(entry.getKey(), value));
            }
            if (settledThisRound.isEmpty()) {
                break;
            }
            settled.putAll(settledThisRound);
            estimated.putAll(settledThisRound);
            rounds++;
        }

        Set<Identifier> unresolved = new TreeSet<>(BY_ID);
        byResult.keySet().stream().filter(item -> !settled.containsKey(item)).forEach(unresolved::add);
        return new Result(Map.copyOf(estimated), Set.copyOf(unresolved), rounds);
    }

    /** Recipes are sorted by id, so the first of equally cheap recipes wins. */
    private static Optional<AspectList> cheapest(
            List<EstimationRecipe> recipes,
            Map<Identifier, AspectList> settled,
            Function<Identifier, Optional<Identifier>> remainderOf) {
        AspectList best = null;
        for (EstimationRecipe recipe : recipes) {
            Optional<AspectList> value = value(recipe, settled, remainderOf);
            if (value.isPresent() && (best == null || value.get().total() < best.total())) {
                best = value.get();
            }
        }
        return Optional.ofNullable(best);
    }

    static Optional<AspectList> value(
            EstimationRecipe recipe,
            Map<Identifier, AspectList> settled,
            Function<Identifier, Optional<Identifier>> remainderOf) {
        AspectList.Builder sum = AspectList.builder();
        for (List<Identifier> slot : recipe.slots()) {
            Optional<AspectList> input = cheapestInput(slot, settled, remainderOf);
            if (input.isEmpty()) {
                return Optional.empty();
            }
            sum.addAll(input.get());
        }
        AspectList value = sum.build()
                .scale(DECAY_NUMERATOR, DECAY_DENOMINATOR * recipe.count())
                .plus(recipe.bonus())
                .limit(MAX_ASPECTS);
        return Optional.of(value);
    }

    private static Optional<AspectList> cheapestInput(
            List<Identifier> options,
            Map<Identifier, AspectList> settled,
            Function<Identifier, Optional<Identifier>> remainderOf) {
        AspectList best = null;
        Identifier bestId = null;
        for (Identifier option : options) {
            AspectList value = settled.get(option);
            if (value == null) {
                continue;
            }
            Optional<Identifier> remainder = remainderOf.apply(option);
            if (remainder.isPresent()) {
                AspectList left = settled.get(remainder.get());
                if (left == null) {
                    continue;
                }
                value = value.minus(left);
            }
            if (best == null
                    || value.total() < best.total()
                    || (value.total() == best.total() && BY_ID.compare(option, bestId) < 0)) {
                best = value;
                bestId = option;
            }
        }
        return Optional.ofNullable(best);
    }
}
