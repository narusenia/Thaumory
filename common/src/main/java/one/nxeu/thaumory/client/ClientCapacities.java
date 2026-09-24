package one.nxeu.thaumory.client;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** The infusion capacities the server sent, and what an item stores for an active effect, for tooltips. */
public final class ClientCapacities {
    private static volatile Map<Identifier, Integer> capacities = Map.of();
    private static volatile int itemEssentia;

    private ClientCapacities() {}

    public static int of(Item item) {
        return capacities.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0);
    }

    /** How much of each aspect an item with an active effect stores. */
    public static int itemEssentia() {
        return itemEssentia;
    }

    static void replace(Map<Identifier, Integer> newCapacities, int newItemEssentia) {
        capacities = Map.copyOf(newCapacities);
        itemEssentia = newItemEssentia;
    }

    static void clear() {
        capacities = Map.of();
        itemEssentia = 0;
    }
}
