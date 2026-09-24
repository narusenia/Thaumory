package one.nxeu.thaumory.infusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;

/**
 * How much of a circle's effect each item can take (requirements §10.2). Resolved like item aspects:
 * an entry for the item itself wins over tag entries, and among the rest the one loaded last wins.
 */
public final class CapacityTable {
    public static final CapacityTable EMPTY = new CapacityTable(Map.of(), List.of());

    private final Map<Identifier, Integer> items;
    private final List<TagEntry> tags;

    private record TagEntry(Identifier tag, int capacity) {}

    private CapacityTable(Map<Identifier, Integer> items, List<TagEntry> tags) {
        this.items = items;
        this.tags = tags;
    }

    /**
     * Builds the table from files in load order. Entries naming an unknown item are skipped with a warning.
     *
     * @param itemExists whether an item id is registered
     */
    public static CapacityTable build(List<Map.Entry<Identifier, CapacityFile>> files, Predicate<Identifier> itemExists,
            Consumer<String> warn) {
        Map<Identifier, Integer> items = new LinkedHashMap<>();
        List<TagEntry> tags = new ArrayList<>();
        for (Map.Entry<Identifier, CapacityFile> file : files) {
            for (CapacityFile.Entry entry : file.getValue().values()) {
                ItemAspectFile.Target target = entry.target();
                if (target.tag()) {
                    tags.add(new TagEntry(target.id(), entry.capacity()));
                } else if (itemExists.test(target.id())) {
                    items.put(target.id(), entry.capacity());
                } else {
                    warn.accept(file.getKey() + ": unknown item " + target + ", skipped");
                }
            }
        }
        return new CapacityTable(Collections.unmodifiableMap(items), List.copyOf(tags));
    }

    /**
     * The item's capacity, 0 when no entry covers it.
     *
     * @param inTag whether the item belongs to a tag id
     */
    public int capacity(Identifier item, Predicate<Identifier> inTag) {
        Integer direct = items.get(item);
        if (direct != null) {
            return direct;
        }
        for (int i = tags.size() - 1; i >= 0; i--) {
            if (inTag.test(tags.get(i).tag())) {
                return tags.get(i).capacity();
            }
        }
        return 0;
    }

    /** Number of item entries plus tag entries. */
    public int size() {
        return items.size() + tags.size();
    }
}
