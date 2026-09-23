package one.nxeu.thaumory.aspect.data;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import one.nxeu.thaumory.api.ThaumoryApi;
import org.slf4j.Logger;

/**
 * Loads {@code data/<namespace>/thaumory/item_aspects/*.json} on every datapack (re)load.
 *
 * <p>Files are applied from the lowest-priority datapack to the highest, and by file id within a
 * pack, so a higher datapack always overrides entries from a lower one. A file at the same path
 * in a higher datapack replaces the lower file entirely, as with vanilla data.
 */
public final class ItemAspectReloadListener
        extends SimplePreparableReloadListener<List<Map.Entry<Identifier, ItemAspectFile>>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter FILES = FileToIdConverter.json("thaumory/item_aspects");

    @Override
    protected List<Map.Entry<Identifier, ItemAspectFile>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, Integer> packPriority = new HashMap<>();
        manager.listPacks().map(PackResources::packId).forEach(id -> packPriority.putIfAbsent(id, packPriority.size()));

        record Loaded(Identifier id, int priority, ItemAspectFile file) {}
        List<Loaded> loaded = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : FILES.listMatchingResources(manager).entrySet()) {
            Identifier id = FILES.fileToId(entry.getKey());
            Resource resource = entry.getValue();
            try (Reader reader = resource.openAsReader()) {
                ItemAspectFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                        .ifSuccess(file -> loaded.add(new Loaded(id, packPriority.getOrDefault(resource.sourcePackId(), -1), file)))
                        .ifError(error -> LOGGER.warn("Skipping item aspect file {} from {}: {}", id, resource.sourcePackId(), error.message()));
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Skipping item aspect file {} from {}", id, resource.sourcePackId(), e);
            }
        }

        loaded.sort(Comparator.comparingInt(Loaded::priority).thenComparing(Loaded::id));
        return loaded.stream().map(l -> Map.entry(l.id(), l.file())).toList();
    }

    @Override
    protected void apply(List<Map.Entry<Identifier, ItemAspectFile>> files, ResourceManager manager, ProfilerFiller profiler) {
        // Every mod has registered its aspects by the time datapacks load.
        ThaumoryApi.aspects().freeze();

        ItemAspectTable table = ItemAspectTable.build(
                files, ThaumoryApi.aspects(), BuiltInRegistries.ITEM::containsKey, LOGGER::warn);
        ItemAspects.update(table);
        LOGGER.info("Loaded {} item aspect entries from {} files", table.size(), files.size());
    }
}
