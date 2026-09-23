package one.nxeu.thaumory.fabric.datagen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;

/** Collects item aspect entries in order; a typo in an item or aspect name fails to compile. */
final class ItemAspectFileBuilder {
    private final List<ItemAspectFile.Entry> entries = new ArrayList<>();

    ItemAspectFileBuilder item(ItemLike item, AspectStack... aspects) {
        return add(new ItemAspectFile.Target(BuiltInRegistries.ITEM.getKey(item.asItem()), false), aspects);
    }

    ItemAspectFileBuilder tag(TagKey<Item> tag, AspectStack... aspects) {
        return add(new ItemAspectFile.Target(tag.location(), true), aspects);
    }

    /** Explicitly no aspects, and never estimated from recipes. */
    ItemAspectFileBuilder none(ItemLike... items) {
        for (ItemLike item : items) {
            item(item);
        }
        return this;
    }

    private ItemAspectFileBuilder add(ItemAspectFile.Target target, AspectStack... aspects) {
        Map<Identifier, Integer> amounts = new LinkedHashMap<>();
        for (AspectStack stack : aspects) {
            amounts.merge(stack.aspect().id(), stack.amount(), Integer::sum);
        }
        entries.add(new ItemAspectFile.Entry(target, amounts));
        return this;
    }

    ItemAspectFile build() {
        return new ItemAspectFile(List.copyOf(entries));
    }
}
