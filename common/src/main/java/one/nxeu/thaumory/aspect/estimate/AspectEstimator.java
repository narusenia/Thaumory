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
 * Estimates what items without a datapack entry are made of, from their recipes (requirements §2.3).
 *
 * <p>Values are first settled in rounds. Round one starts from the hand-written items. Each round,
 * every unsettled item that has a recipe whose inputs are all settled gets the value of its
 * cheapest such recipe, and all of them settle together at the end of the round.
 *
 * <p>Settled values are then lowered until no recipe makes an item for less than its value, so a
 * route found in a later round still counts (sticks from planks beat sticks from bamboo). At that
 * point no recipe turns aspects into more aspects: an item is never worth more than any way of
 * making it. Nothing decays here; melting crafted items at a loss is the crucible's job.
 *
 * <p>One recipe is worth {@code sum of inputs / count + bonus}, limited to the six largest aspects.
 * Each slot uses its cheapest item other than the result itself; an item that leaves a remainder
 * (a bucket from a milk bucket) counts as itself minus the remainder. Amounts are kept
 * {@link #PRECISION} times finer while estimating and rounded down once at the end, so fractions
 * such as a ninth of an ingot are not lost along the way; lowering has to beat the current value by
 * {@link #TOLERANCE} so rounding alone never counts as a cheaper route.
 */
public final class AspectEstimator {
    static final int MAX_ASPECTS = 6;
    static final int PRECISION = 1000;
    static final int TOLERANCE = PRECISION / 10;

    private static final Comparator<Identifier> BY_ID = Comparator.comparing(Identifier::toString);

    /**
     * @param rounds settling rounds before lowering
     * @param lowered how many times a settled value was lowered by a cheaper route
     * @param fell items whose final total is under half of their first settled total
     */
    public record Result(
            Map<Identifier, AspectList> estimated, Set<Identifier> unresolved, int rounds, int lowered, Map<Identifier, Fall> fell) {}

    public record Fall(int first, int last) {}

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

        Map<Identifier, AspectList> settled = new HashMap<>();
        manual.forEach((item, value) -> settled.put(item, fine(value)));

        int rounds = 0;
        while (true) {
            Map<Identifier, AspectList> settledThisRound = new HashMap<>();
            for (Map.Entry<Identifier, List<EstimationRecipe>> entry : byResult.entrySet()) {
                if (!settled.containsKey(entry.getKey())) {
                    cheapest(entry.getKey(), entry.getValue(), settled, remainderOf)
                            .ifPresent(value -> settledThisRound.put(entry.getKey(), value));
                }
            }
            if (settledThisRound.isEmpty()) {
                break;
            }
            settled.putAll(settledThisRound);
            rounds++;
        }
        Map<Identifier, Integer> firstTotals = new HashMap<>();
        byResult.keySet().forEach(item -> Optional.ofNullable(settled.get(item))
                .ifPresent(value -> firstTotals.put(item, value.total())));

        // Totals only ever drop, so this ends.
        int lowered = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<Identifier, List<EstimationRecipe>> entry : byResult.entrySet()) {
                AspectList current = settled.get(entry.getKey());
                if (current == null) {
                    continue;
                }
                Optional<AspectList> better = cheapest(entry.getKey(), entry.getValue(), settled, remainderOf)
                        .filter(candidate -> candidate.total() + TOLERANCE < current.total());
                if (better.isPresent()) {
                    settled.put(entry.getKey(), better.get());
                    lowered++;
                    changed = true;
                }
            }
        }

        Map<Identifier, AspectList> estimated = new HashMap<>();
        Map<Identifier, Fall> fell = new HashMap<>();
        Set<Identifier> unresolved = new TreeSet<>(BY_ID);
        for (Identifier item : byResult.keySet()) {
            AspectList value = settled.get(item);
            if (value == null) {
                unresolved.add(item);
                continue;
            }
            estimated.put(item, coarse(value));
            int first = firstTotals.get(item);
            if (value.total() * 2 < first) {
                fell.put(item, new Fall(first / PRECISION, value.total() / PRECISION));
            }
        }
        return new Result(Map.copyOf(estimated), Set.copyOf(unresolved), rounds, lowered, Map.copyOf(fell));
    }

    private static AspectList fine(AspectList value) {
        return value.scale(PRECISION, 1);
    }

    private static AspectList coarse(AspectList value) {
        return value.scale(1, PRECISION);
    }

    /** Recipes are sorted by id, so the first of equally cheap recipes wins. */
    private static Optional<AspectList> cheapest(
            Identifier result,
            List<EstimationRecipe> recipes,
            Map<Identifier, AspectList> settled,
            Function<Identifier, Optional<Identifier>> remainderOf) {
        AspectList best = null;
        for (EstimationRecipe recipe : recipes) {
            Optional<AspectList> value = value(result, recipe, settled, remainderOf);
            if (value.isPresent() && (best == null || value.get().total() < best.total())) {
                best = value.get();
            }
        }
        return Optional.ofNullable(best);
    }

    private static Optional<AspectList> value(
            Identifier result,
            EstimationRecipe recipe,
            Map<Identifier, AspectList> settled,
            Function<Identifier, Optional<Identifier>> remainderOf) {
        AspectList.Builder sum = AspectList.builder();
        for (List<Identifier> slot : recipe.slots()) {
            Optional<AspectList> input = cheapestInput(result, slot, settled, remainderOf);
            if (input.isEmpty()) {
                return Optional.empty();
            }
            sum.addAll(input.get());
        }
        AspectList value = sum.build()
                .scale(1, recipe.count())
                .plus(fine(recipe.bonus()))
                .limit(MAX_ASPECTS);
        return Optional.of(value);
    }

    /** The cheapest settled item for a slot. The result itself never counts as its own input. */
    private static Optional<AspectList> cheapestInput(
            Identifier result,
            List<Identifier> options,
            Map<Identifier, AspectList> settled,
            Function<Identifier, Optional<Identifier>> remainderOf) {
        AspectList best = null;
        Identifier bestId = null;
        for (Identifier option : options) {
            AspectList value = settled.get(option);
            if (value == null || option.equals(result)) {
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
