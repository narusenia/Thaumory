package one.nxeu.thaumory.infusion;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;

/** The Essentia an item stores for its active effect (requirements §10.2): what it takes in and what a use pays. */
public final class ItemEssentia {
    private ItemEssentia() {}

    /**
     * @param stored what the item holds afterwards
     * @param taken  what went in, to be taken from the source
     */
    public record Fill(AspectList stored, AspectList taken) {}

    /** Takes from {@code offered} only the aspects the item stores, each up to {@code capacity}. */
    public static Fill fill(AspectList stored, AspectList offered, Set<Identifier> accepted, int capacity) {
        AspectList.Builder taken = AspectList.builder();
        for (AspectStack stack : offered.stacks()) {
            Aspect aspect = stack.aspect();
            if (!accepted.contains(aspect.id())) {
                continue;
            }
            int room = Math.max(0, capacity - stored.amount(aspect));
            int amount = Math.min(room, stack.amount());
            if (amount > 0) {
                taken.add(aspect, amount);
            }
        }
        AspectList took = taken.build();
        return new Fill(stored.plus(took), took);
    }

    /** What is left after paying {@code cost}, or empty when some aspect runs short (then nothing is paid). */
    public static Optional<AspectList> pay(AspectList stored, Map<Identifier, Integer> cost, AspectRegistry registry) {
        AspectList.Builder needed = AspectList.builder();
        for (Map.Entry<Identifier, Integer> entry : cost.entrySet()) {
            Optional<Aspect> aspect = registry.get(entry.getKey());
            if (aspect.isEmpty()) {
                return Optional.empty();
            }
            needed.add(aspect.get(), entry.getValue());
        }
        AspectList total = needed.build();
        return stored.containsAll(total) ? Optional.of(stored.minus(total)) : Optional.empty();
    }
}
