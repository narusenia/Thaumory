package one.nxeu.thaumory.client;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** The infusion capacities the server sent, for tooltips. */
public final class ClientCapacities {
    private static volatile Map<Identifier, Integer> capacities = Map.of();

    private ClientCapacities() {}

    public static int of(Item item) {
        return capacities.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0);
    }

    static void replace(Map<Identifier, Integer> newCapacities) {
        capacities = Map.copyOf(newCapacities);
    }

    static void clear() {
        capacities = Map.of();
    }
}
