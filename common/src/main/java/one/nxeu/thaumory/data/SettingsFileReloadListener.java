package one.nxeu.thaumory.data;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Loads one settings file, such as {@code data/thaumory/thaumory/flux.json}, from the highest
 * datapack that has it. Falls back to the defaults when the file is missing or invalid.
 */
public final class SettingsFileReloadListener<T> extends SimplePreparableReloadListener<T> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Identifier file;
    private final Codec<T> codec;
    private final T defaults;
    private final Consumer<T> apply;

    /** @param file the path under {@code data/}, e.g. {@code thaumory:thaumory/flux.json} */
    public SettingsFileReloadListener(Identifier file, Codec<T> codec, T defaults, Consumer<T> apply) {
        this.file = file;
        this.codec = codec;
        this.defaults = defaults;
        this.apply = apply;
    }

    @Override
    protected T prepare(ResourceManager manager, ProfilerFiller profiler) {
        Optional<Resource> resource = manager.getResource(file);
        if (resource.isEmpty()) {
            LOGGER.warn("No {} found; using defaults", file);
            return defaults;
        }
        String pack = resource.get().sourcePackId();
        try (Reader reader = resource.get().openAsReader()) {
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .resultOrPartial(error -> LOGGER.warn("Invalid {} from {}; using defaults: {}", file, pack, error))
                    .orElse(defaults);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {} from {}; using defaults", file, pack, e);
            return defaults;
        }
    }

    @Override
    protected void apply(T settings, ResourceManager manager, ProfilerFiller profiler) {
        apply.accept(settings);
    }
}
