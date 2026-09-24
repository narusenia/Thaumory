package one.nxeu.thaumory.infusion;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.data.OrderedDataFiles;
import org.slf4j.Logger;

/** Each item's infusion capacity on the server, from {@code data/<namespace>/thaumory/infusion_capacity/*.json}. */
public final class InfusionCapacities {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter FILES = FileToIdConverter.json("thaumory/infusion_capacity");

    private static volatile CapacityTable table = CapacityTable.EMPTY;
    private static final Map<Item, Integer> CACHE = new ConcurrentHashMap<>();

    private InfusionCapacities() {}

    /** How much of a circle's effect the item can take; 0 when it takes none. */
    public static int of(Item item) {
        return CACHE.computeIfAbsent(item, InfusionCapacities::lookup);
    }

    private static int lookup(Item item) {
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(item);
        return table.capacity(BuiltInRegistries.ITEM.getKey(item), tag -> holder.is(TagKey.create(Registries.ITEM, tag)));
    }

    /** Every item with a capacity, for the clients. */
    public static Map<Identifier, Integer> snapshot() {
        Map<Identifier, Integer> capacities = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            int capacity = of(item);
            if (capacity > 0) {
                capacities.put(BuiltInRegistries.ITEM.getKey(item), capacity);
            }
        }
        return capacities;
    }

    /** Forgets cached lookups, e.g. after tags change. */
    public static void invalidate() {
        CACHE.clear();
    }

    public static SimplePreparableReloadListener<List<Map.Entry<Identifier, CapacityFile>>> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected List<Map.Entry<Identifier, CapacityFile>> prepare(ResourceManager manager, ProfilerFiller profiler) {
                return OrderedDataFiles.load(manager, FILES, CapacityFile.CODEC, "infusion capacity", LOGGER);
            }

            @Override
            protected void apply(List<Map.Entry<Identifier, CapacityFile>> files, ResourceManager manager, ProfilerFiller profiler) {
                table = CapacityTable.build(files, BuiltInRegistries.ITEM::containsKey, LOGGER::warn);
                CACHE.clear();
                LOGGER.info("Loaded {} infusion capacity entries from {} files", table.size(), files.size());
            }
        };
    }
}
