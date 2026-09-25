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
import one.nxeu.thaumory.data.OrderedDataFiles;
import org.slf4j.Logger;

/**
 * The wand parts the datapacks define, by item (requirements §7.1). A later file for the same item
 * replaces an earlier one. Server side; clients get a copy for tooltips.
 */
public final class WandParts {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter FILES = FileToIdConverter.json("thaumory/wand_part");
    private static volatile Map<Identifier, WandPart> parts = Map.of();

    private WandParts() {}

    public static Map<Identifier, WandPart> snapshot() {
        return parts;
    }

    /** Keyed by item; a later part for the same item replaces the earlier. */
    public static Map<Identifier, WandPart> table(List<WandPart> loaded) {
        Map<Identifier, WandPart> table = new LinkedHashMap<>();
        for (WandPart part : loaded) {
            table.put(part.item(), part);
        }
        return Map.copyOf(table);
    }

    public static Optional<WandPart.Cap> cap(Map<Identifier, WandPart> table, Identifier item) {
        return Optional.ofNullable(table.get(item)).flatMap(WandPart::cap);
    }

    public static Optional<WandPart.Core> core(Map<Identifier, WandPart> table, Identifier item) {
        return Optional.ofNullable(table.get(item)).flatMap(WandPart::core);
    }

    /** What {@code item} is in a wand recipe, going by the parts loaded now. */
    public static WandCrafting.Role role(Identifier item, Identifier wand) {
        if (item.equals(wand)) {
            return WandCrafting.Role.WAND;
        }
        WandPart part = parts.get(item);
        if (part == null) {
            return WandCrafting.Role.OTHER;
        }
        return part.cap().isPresent() ? WandCrafting.Role.CAP : WandCrafting.Role.CORE;
    }

    public static SimplePreparableReloadListener<List<Map.Entry<Identifier, WandPart>>> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected List<Map.Entry<Identifier, WandPart>> prepare(ResourceManager manager, ProfilerFiller profiler) {
                return OrderedDataFiles.load(manager, FILES, WandPart.CODEC, "wand part", LOGGER);
            }

            @Override
            protected void apply(List<Map.Entry<Identifier, WandPart>> files, ResourceManager manager, ProfilerFiller profiler) {
                List<WandPart> known = files.stream().map(Map.Entry::getValue).filter(part -> {
                    if (BuiltInRegistries.ITEM.containsKey(part.item())) {
                        return true;
                    }
                    LOGGER.warn("Ignoring wand part for unknown item {}", part.item());
                    return false;
                }).toList();
                parts = table(known);
                LOGGER.info("Loaded {} wand parts", parts.size());
            }
        };
    }
}
