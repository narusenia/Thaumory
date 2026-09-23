package one.nxeu.thaumory.aspect.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;

/**
 * Hand-written item aspects loaded from datapacks (requirements §2.3).
 *
 * <p>An entry for the item itself wins over tag entries. Among entries for the same item, or among
 * tags that contain the item, the one loaded last wins. {@link ItemAspectReloadListener} decides
 * the load order.
 */
public final class ItemAspectTable {
    public static final ItemAspectTable EMPTY = new ItemAspectTable(Map.of(), List.of());

    private final Map<Identifier, AspectList> items;
    private final List<TagEntry> tags;

    private record TagEntry(Identifier tag, AspectList aspects) {}

    private ItemAspectTable(Map<Identifier, AspectList> items, List<TagEntry> tags) {
        this.items = items;
        this.tags = tags;
    }

    /**
     * Builds the table from files in load order. Entries naming an unknown aspect or item are
     * skipped with a warning.
     *
     * @param itemExists whether an item id is registered
     */
    public static ItemAspectTable build(
            List<Map.Entry<Identifier, ItemAspectFile>> files,
            AspectRegistry registry,
            Predicate<Identifier> itemExists,
            Consumer<String> warn) {
        Map<Identifier, AspectList> items = new LinkedHashMap<>();
        List<TagEntry> tags = new ArrayList<>();
        for (Map.Entry<Identifier, ItemAspectFile> file : files) {
            for (ItemAspectFile.Entry entry : file.getValue().values()) {
                ItemAspectFile.Target target = entry.target();
                Optional<AspectList> aspects = resolve(entry.aspects(), registry, file.getKey(), target, warn);
                if (aspects.isEmpty()) {
                    continue;
                }
                if (target.tag()) {
                    tags.add(new TagEntry(target.id(), aspects.get()));
                } else if (itemExists.test(target.id())) {
                    items.put(target.id(), aspects.get());
                } else {
                    warn.accept(file.getKey() + ": unknown item " + target + ", skipped");
                }
            }
        }
        return new ItemAspectTable(Collections.unmodifiableMap(items), List.copyOf(tags));
    }

    private static Optional<AspectList> resolve(
            Map<Identifier, Integer> amounts,
            AspectRegistry registry,
            Identifier file,
            ItemAspectFile.Target target,
            Consumer<String> warn) {
        AspectList.Builder builder = AspectList.builder();
        for (Map.Entry<Identifier, Integer> amount : amounts.entrySet()) {
            Optional<Aspect> aspect = registry.get(amount.getKey());
            if (aspect.isEmpty()) {
                warn.accept(file + ": unknown aspect " + amount.getKey() + " for " + target + ", skipped");
                return Optional.empty();
            }
            builder.add(aspect.get(), amount.getValue());
        }
        return Optional.of(builder.build());
    }

    /**
     * The hand-written aspects for an item, or empty if no entry covers it. A present but empty
     * list means the item explicitly has no aspects.
     *
     * @param inTag whether the item belongs to a tag id
     */
    public Optional<AspectList> lookup(Identifier item, Predicate<Identifier> inTag) {
        AspectList direct = items.get(item);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (int i = tags.size() - 1; i >= 0; i--) {
            TagEntry entry = tags.get(i);
            if (inTag.test(entry.tag())) {
                return Optional.of(entry.aspects());
            }
        }
        return Optional.empty();
    }

    /** Number of item entries plus tag entries. */
    public int size() {
        return items.size() + tags.size();
    }
}
