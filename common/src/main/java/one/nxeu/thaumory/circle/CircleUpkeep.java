package one.nxeu.thaumory.circle;

import java.util.Optional;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;

/**
 * What a circle takes from its Core's Essentia. A triggered circle pays its cost from both effect
 * runes and 1 from its slot 3 rune per activation; a sustained one pays 1 from each effect rune
 * every interval. Nothing is taken unless all of it can be.
 */
public final class CircleUpkeep {
    private CircleUpkeep() {}

    /** Essentia per effect rune for one activation: {@code cost} scaled and rounded, at least 1. */
    public static int triggeredCost(int cost, double costMultiplier) {
        return Math.max(1, (int) Math.round(cost * costMultiplier));
    }

    /** Ticks between payments of a sustained circle: the cheaper it runs, the longer between. */
    public static int sustainedInterval(int interval, double costMultiplier) {
        return Math.max(1, (int) Math.round(interval / costMultiplier));
    }

    /** @return what the Core holds after one activation, or empty if it cannot pay */
    public static Optional<AspectList> payTriggered(AspectList stored, Aspect first, Aspect second, Optional<Aspect> parameter, int cost) {
        AspectList.Builder needed = AspectList.builder().add(first, cost).add(second, cost);
        parameter.ifPresent(aspect -> needed.add(aspect, 1));
        return pay(stored, needed.build());
    }

    /** @return what the Core holds after one interval's payment, or empty if it cannot pay */
    public static Optional<AspectList> paySustained(AspectList stored, Aspect first, Aspect second) {
        return pay(stored, AspectList.builder().add(first, 1).add(second, 1).build());
    }

    /**
     * @return what the Core holds after an effect pays {@code amount}, scaled by the cost multiplier
     *     and rounded (at least 1), from each effect rune for work it does, or empty if it cannot pay
     */
    public static Optional<AspectList> payWork(AspectList stored, Aspect first, Aspect second, int amount, double costMultiplier) {
        int each = triggeredCost(amount, costMultiplier);
        return pay(stored, AspectList.builder().add(first, each).add(second, each).build());
    }

    private static Optional<AspectList> pay(AspectList stored, AspectList needed) {
        return stored.containsAll(needed) ? Optional.of(stored.minus(needed)) : Optional.empty();
    }
}
