package one.nxeu.thaumory.api.aspect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An immutable set of aspects with amounts, such as an item's composition or a crucible's
 * contents. Aspects with no amount are never stored.
 *
 * <p>Iteration order is insertion order. Use {@link #sortedByAmount()} for "largest first".
 */
public final class AspectList {
    private static final AspectList EMPTY = new AspectList(Map.of());

    /** Largest amount first; ties broken by id so the order is stable across runs. */
    private static final Comparator<AspectStack> BY_AMOUNT = Comparator
            .comparingInt(AspectStack::amount).reversed()
            .thenComparing(stack -> stack.aspect().id().toString());

    private final Map<Aspect, Integer> amounts;

    private AspectList(Map<Aspect, Integer> amounts) {
        this.amounts = amounts;
    }

    public static AspectList empty() {
        return EMPTY;
    }

    public static AspectList of(Aspect aspect, int amount) {
        return builder().add(aspect, amount).build();
    }

    public static AspectList of(AspectStack... stacks) {
        Builder builder = builder();
        for (AspectStack stack : stacks) {
            builder.add(stack.aspect(), stack.amount());
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public int amount(Aspect aspect) {
        return amounts.getOrDefault(aspect, 0);
    }

    public boolean contains(Aspect aspect) {
        return amounts.containsKey(aspect);
    }

    /** True if this list has at least as much of every aspect in {@code other}. */
    public boolean containsAll(AspectList other) {
        return other.amounts.entrySet().stream().allMatch(e -> amount(e.getKey()) >= e.getValue());
    }

    public boolean isEmpty() {
        return amounts.isEmpty();
    }

    /** Number of distinct aspects. */
    public int size() {
        return amounts.size();
    }

    /** Sum of all amounts. */
    public int total() {
        return amounts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Stacks in insertion order. */
    public List<AspectStack> stacks() {
        List<AspectStack> stacks = new ArrayList<>(amounts.size());
        amounts.forEach((aspect, amount) -> stacks.add(new AspectStack(aspect, amount)));
        return List.copyOf(stacks);
    }

    /** Stacks with the largest amount first. */
    public List<AspectStack> sortedByAmount() {
        return stacks().stream().sorted(BY_AMOUNT).toList();
    }

    /** The stack with the largest amount. */
    public Optional<AspectStack> largest() {
        return stacks().stream().min(BY_AMOUNT);
    }

    public AspectList plus(AspectList other) {
        return builder().addAll(this).addAll(other).build();
    }

    /** Subtracts {@code other}. Amounts that would go below zero are removed. */
    public AspectList minus(AspectList other) {
        Builder builder = builder();
        amounts.forEach((aspect, amount) -> builder.add(aspect, amount - other.amount(aspect)));
        return builder.build();
    }

    /**
     * Multiplies every amount by {@code numerator / denominator}, rounding down, and drops aspects
     * that reach zero. Integer math keeps results exact, e.g. {@code scale(3, 4)} for 75%.
     */
    public AspectList scale(long numerator, long denominator) {
        if (numerator < 0 || denominator <= 0) {
            throw new IllegalArgumentException("Invalid scale " + numerator + "/" + denominator);
        }
        Builder builder = builder();
        amounts.forEach((aspect, amount) -> builder.add(aspect, Math.toIntExact(amount * numerator / denominator)));
        return builder.build();
    }

    /** Keeps the {@code maxAspects} largest stacks and drops the rest. */
    public AspectList limit(int maxAspects) {
        if (maxAspects < 0) {
            throw new IllegalArgumentException("maxAspects must not be negative: " + maxAspects);
        }
        if (size() <= maxAspects) {
            return this;
        }
        List<AspectStack> kept = sortedByAmount().subList(0, maxAspects);
        Builder builder = builder();
        // Rebuild in insertion order so limiting does not reorder what remains.
        amounts.forEach((aspect, amount) -> {
            if (kept.stream().anyMatch(stack -> stack.aspect().equals(aspect))) {
                builder.add(aspect, amount);
            }
        });
        return builder.build();
    }

    /**
     * Pairs of aspects in this list that cancel each other out (requirements §3): their primal
     * breakdowns contain opposite primals.
     */
    public List<List<Aspect>> cancellingPairs(AspectRegistry registry) {
        List<Aspect> aspects = List.copyOf(amounts.keySet());
        List<List<Aspect>> pairs = new ArrayList<>();
        for (int i = 0; i < aspects.size(); i++) {
            for (int j = i + 1; j < aspects.size(); j++) {
                if (registry.cancels(aspects.get(i), aspects.get(j))) {
                    pairs.add(List.of(aspects.get(i), aspects.get(j)));
                }
            }
        }
        return List.copyOf(pairs);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AspectList list && amounts.equals(list.amounts);
    }

    @Override
    public int hashCode() {
        return amounts.hashCode();
    }

    @Override
    public String toString() {
        return stacks().toString();
    }

    /** Accumulates amounts. Adding zero or a negative amount is ignored. */
    public static final class Builder {
        private final Map<Aspect, Integer> amounts = new LinkedHashMap<>();

        private Builder() {}

        public Builder add(Aspect aspect, int amount) {
            if (amount > 0) {
                amounts.merge(aspect, amount, Math::addExact);
            }
            return this;
        }

        public Builder addAll(AspectList list) {
            list.amounts.forEach(this::add);
            return this;
        }

        public AspectList build() {
            return amounts.isEmpty() ? EMPTY : new AspectList(Collections.unmodifiableMap(new LinkedHashMap<>(amounts)));
        }
    }
}
