package one.nxeu.thaumory.wand;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.data.OrderedDataFiles;
import org.slf4j.Logger;

/**
 * The wand foci the datapacks define, by item (requirements §17.7). A later file for the same item
 * replaces an earlier one. Server side; clients get a copy for tooltips and the focus menu.
 */
public final class WandFoci {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter FILES = FileToIdConverter.json("thaumory/wand_focus");
    private static volatile Map<Identifier, WandFocus> foci = Map.of();

    private WandFoci() {}

    public static Map<Identifier, WandFocus> snapshot() {
        return foci;
    }

    /** Keyed by item; a later focus for the same item replaces the earlier. */
    public static Map<Identifier, WandFocus> table(List<WandFocus> loaded) {
        Map<Identifier, WandFocus> table = new LinkedHashMap<>();
        for (WandFocus focus : loaded) {
            table.put(focus.item(), focus);
        }
        return Map.copyOf(table);
    }

    /** The focus {@code stack} is, going by {@code table}. */
    public static Optional<WandFocus> of(Map<Identifier, WandFocus> table, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    /** The focus {@code stack} is, going by the foci loaded now. */
    public static Optional<WandFocus> of(ItemStack stack) {
        return of(foci, stack);
    }

    public static SimplePreparableReloadListener<List<Map.Entry<Identifier, WandFocus>>> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected List<Map.Entry<Identifier, WandFocus>> prepare(ResourceManager manager, ProfilerFiller profiler) {
                return OrderedDataFiles.load(manager, FILES, WandFocus.CODEC, "wand focus", LOGGER);
            }

            @Override
            protected void apply(List<Map.Entry<Identifier, WandFocus>> files, ResourceManager manager, ProfilerFiller profiler) {
                List<WandFocus> known = files.stream().map(Map.Entry::getValue).filter(focus -> {
                    if (!BuiltInRegistries.ITEM.containsKey(focus.item())) {
                        LOGGER.warn("Ignoring wand focus for unknown item {}", focus.item());
                        return false;
                    }
                    if (!ThaumoryApi.focusSpells().contains(focus.effect())) {
                        LOGGER.warn("Ignoring wand focus {} with unknown spell {}", focus.item(), focus.effect());
                        return false;
                    }
                    return true;
                }).toList();
                foci = table(known);
                LOGGER.info("Loaded {} wand foci", foci.size());
            }
        };
    }
}
