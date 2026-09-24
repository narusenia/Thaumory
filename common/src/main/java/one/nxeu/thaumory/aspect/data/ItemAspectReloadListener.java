package one.nxeu.thaumory.aspect.data;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.data.OrderedDataFiles;
import org.slf4j.Logger;

/**
 * Loads {@code data/<namespace>/thaumory/item_aspects/*.json} on every datapack (re)load.
 *
 * <p>A higher datapack overrides entries from a lower one ({@link OrderedDataFiles}).
 */
public final class ItemAspectReloadListener
        extends SimplePreparableReloadListener<List<Map.Entry<Identifier, ItemAspectFile>>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter FILES = FileToIdConverter.json("thaumory/item_aspects");

    @Override
    protected List<Map.Entry<Identifier, ItemAspectFile>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return OrderedDataFiles.load(manager, FILES, ItemAspectFile.CODEC, "item aspect", LOGGER);
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
