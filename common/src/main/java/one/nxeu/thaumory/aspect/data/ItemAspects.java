package one.nxeu.thaumory.aspect.data;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.aspect.AspectList;

/**
 * Aspect lookup for items at runtime: the datapack entry if there is one, otherwise the value
 * estimated from recipes. Datapack lookups are cached until the next reload.
 */
public final class ItemAspects {
    private static volatile ItemAspectTable table = ItemAspectTable.EMPTY;
    private static volatile Map<Identifier, AspectList> estimated = Map.of();
    private static final Map<Item, Optional<AspectList>> MANUAL_CACHE = new ConcurrentHashMap<>();

    public enum Source { DATAPACK, RECIPE, NONE }

    private ItemAspects() {}

    public static AspectList get(ItemStack stack) {
        return stack.isEmpty() ? AspectList.empty() : get(stack.getItem());
    }

    public static AspectList get(Item item) {
        return manual(item).orElseGet(() -> estimated.getOrDefault(BuiltInRegistries.ITEM.getKey(item), AspectList.empty()));
    }

    public static Source source(Item item) {
        if (manual(item).isPresent()) {
            return Source.DATAPACK;
        }
        return estimated.containsKey(BuiltInRegistries.ITEM.getKey(item)) ? Source.RECIPE : Source.NONE;
    }

    /** The datapack entry covering this item, if any. */
    public static Optional<AspectList> manual(Item item) {
        return MANUAL_CACHE.computeIfAbsent(item, ItemAspects::lookup);
    }

    private static Optional<AspectList> lookup(Item item) {
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        return table.lookup(
                BuiltInRegistries.ITEM.getKey(item),
                tag -> holder.is(TagKey.create(Registries.ITEM, tag)));
    }

    static void update(ItemAspectTable newTable) {
        table = newTable;
        MANUAL_CACHE.clear();
    }

    public static void updateEstimated(Map<Identifier, AspectList> newEstimated) {
        estimated = Map.copyOf(newEstimated);
    }

    /** Forgets cached datapack lookups, e.g. after tags change. */
    public static void invalidate() {
        MANUAL_CACHE.clear();
    }
}
