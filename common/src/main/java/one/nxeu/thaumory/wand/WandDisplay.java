package one.nxeu.thaumory.wand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * What a wand shows of its Essentia, in its tooltip and its HUD (requirements §17.7): the aspects its
 * focus uses first, even when empty, then whatever else it holds, each group by id.
 */
public final class WandDisplay {
    private WandDisplay() {}

    /**
     * One aspect of the wand.
     *
     * @param used  whether the focus pays with it
     * @param shortOf whether the focus pays with it and the wand holds less than one cast's worth
     */
    public record Bar(Identifier aspect, int amount, boolean used, boolean shortOf) {}

    /**
     * @param cost   what one cast of the focus takes, empty without a focus
     * @param stored what the wand holds
     */
    public static List<Bar> bars(Map<Identifier, Integer> cost, Map<Identifier, Integer> stored) {
        List<Bar> bars = new ArrayList<>();
        cost.keySet().stream().sorted().forEach(aspect -> {
            int amount = stored.getOrDefault(aspect, 0);
            bars.add(new Bar(aspect, amount, true, amount < cost.get(aspect)));
        });
        stored.entrySet().stream()
                .filter(entry -> !cost.containsKey(entry.getKey()) && entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(aspect -> bars.add(new Bar(aspect, stored.get(aspect), false, false)));
        return bars;
    }
}
