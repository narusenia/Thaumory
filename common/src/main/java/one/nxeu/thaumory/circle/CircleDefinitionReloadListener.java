package one.nxeu.thaumory.circle;

import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import one.nxeu.thaumory.api.ThaumoryApi;
import org.slf4j.Logger;

/** Loads {@code data/<namespace>/thaumory/circle/*.json} on every datapack (re)load. */
public final class CircleDefinitionReloadListener extends SimpleJsonResourceReloadListener<CircleDefinitionFile> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile CircleDefinitions definitions = CircleDefinitions.EMPTY;

    public CircleDefinitionReloadListener() {
        super(CircleDefinitionFile.CODEC, FileToIdConverter.json("thaumory/circle"));
    }

    /** The combinations as of the last load. */
    public static CircleDefinitions definitions() {
        return definitions;
    }

    @Override
    protected void apply(Map<Identifier, CircleDefinitionFile> files, ResourceManager manager, ProfilerFiller profiler) {
        ThaumoryApi.aspects().freeze();
        definitions = CircleDefinitions.build(files, ThaumoryApi.aspects(), ThaumoryApi.circleEffects()::contains, LOGGER::warn);
        LOGGER.info("Loaded {} circle combinations", definitions.size());
    }
}
