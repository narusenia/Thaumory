package one.nxeu.thaumory.circle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;

/** The numbers of an infusion (requirements §10.1): what it takes, how likely it fails, the level it gives. */
public final class InfusionRules {
    private InfusionRules() {}

    /**
     * The Essentia an infusion takes: the combination's cost for each effect rune and for slot 3,
     * each scaled by the cost multiplier and rounded, at least 1.
     */
    public static AspectList cost(InfusionCost cost, double costMultiplier, Aspect first, Aspect second, Optional<Aspect> parameter) {
        int runes = CircleUpkeep.triggeredCost(cost.runes(), costMultiplier);
        AspectList.Builder needed = AspectList.builder().add(first, runes).add(second, runes);
        parameter.ifPresent(aspect -> needed.add(aspect, CircleUpkeep.triggeredCost(cost.parameter(), costMultiplier)));
        return needed.build();
    }

    /**
     * What one use of an active effect takes from the Essentia in the item: the combination's item cost
     * from each of its two runes' aspects, scaled by the cost multiplier and rounded, at least 1.
     */
    public static Map<Identifier, Integer> useCost(int itemCost, double costMultiplier, Aspect first, Aspect second) {
        int each = CircleUpkeep.triggeredCost(itemCost, costMultiplier);
        Map<Identifier, Integer> cost = new LinkedHashMap<>();
        cost.merge(first.id(), each, Integer::sum);
        cost.merge(second.id(), each, Integer::sum);
        return cost;
    }

    /** Level 1, and one more for each 0.5 of strength over 1, up to the settings' maximum. */
    public static int level(double strength, InfusionSettings settings) {
        int level = 1 + (int) Math.floor((strength - 1) / 0.5 + 1e-9);
        return Math.clamp(level, 1, settings.maxLevel());
    }

    /** The base chance, plus each point of instability over the threshold, at most certain. */
    public static double failureChance(int instability, int threshold, InfusionSettings settings) {
        int excess = Math.max(0, instability - threshold);
        return Math.min(1, settings.baseFailure() + excess * settings.failurePerPoint());
    }

    /** The Flux a failed infusion releases: its share of the Essentia lost, rounded up. */
    public static int failureFlux(AspectList lost, InfusionSettings settings) {
        return (int) Math.ceil(lost.total() * settings.fluxRatio() - 1e-9);
    }
}
