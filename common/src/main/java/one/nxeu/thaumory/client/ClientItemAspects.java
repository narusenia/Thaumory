package one.nxeu.thaumory.client;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.api.aspect.AspectList;

/** Item aspects as last received from the server. Kept apart from the server's copy on purpose. */
public final class ClientItemAspects {
    private static volatile Map<Identifier, AspectList> items = Map.of();

    private ClientItemAspects() {}

    public static AspectList get(Item item) {
        return items.getOrDefault(BuiltInRegistries.ITEM.getKey(item), AspectList.empty());
    }

    public static int size() {
        return items.size();
    }

    static void replace(Map<Identifier, AspectList> newItems) {
        items = Map.copyOf(newItems);
    }

    static void clear() {
        items = Map.of();
    }
}
