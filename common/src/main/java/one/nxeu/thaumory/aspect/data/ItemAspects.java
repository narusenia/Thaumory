package one.nxeu.thaumory.aspect.data;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.aspect.AspectList;

/** Aspect lookup for items at runtime. Results are cached until the next datapack reload. */
public final class ItemAspects {
    private static volatile ItemAspectTable table = ItemAspectTable.EMPTY;
    private static final Map<Item, Optional<AspectList>> CACHE = new ConcurrentHashMap<>();

    private ItemAspects() {}

    public static AspectList get(ItemStack stack) {
        return stack.isEmpty() ? AspectList.empty() : get(stack.getItem());
    }

    public static AspectList get(Item item) {
        return manual(item).orElse(AspectList.empty());
    }

    /** The datapack entry covering this item, if any. */
    public static Optional<AspectList> manual(Item item) {
        return CACHE.computeIfAbsent(item, ItemAspects::lookup);
    }

    private static Optional<AspectList> lookup(Item item) {
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        return table.lookup(
                BuiltInRegistries.ITEM.getKey(item),
                tag -> holder.is(TagKey.create(Registries.ITEM, tag)));
    }

    static void update(ItemAspectTable newTable) {
        table = newTable;
        CACHE.clear();
    }
}
