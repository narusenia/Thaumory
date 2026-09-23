package one.nxeu.thaumory.flux;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import one.nxeu.thaumory.Thaumory;
import org.slf4j.Logger;

/**
 * Loads {@code data/thaumory/thaumory/flux.json} from the highest datapack that has it. Falls back
 * to {@link FluxSettings#DEFAULT} when the file is missing or invalid.
 */
public final class FluxSettingsReloadListener extends SimplePreparableReloadListener<FluxSettings> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier FILE = Thaumory.id("thaumory/flux.json");

    private final FluxManager flux;

    public FluxSettingsReloadListener(FluxManager flux) {
        this.flux = flux;
    }

    @Override
    protected FluxSettings prepare(ResourceManager manager, ProfilerFiller profiler) {
        Optional<Resource> resource = manager.getResource(FILE);
        if (resource.isEmpty()) {
            LOGGER.warn("No {} found; using default Flux settings", FILE);
            return FluxSettings.DEFAULT;
        }
        String pack = resource.get().sourcePackId();
        try (Reader reader = resource.get().openAsReader()) {
            return FluxSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                    .resultOrPartial(error -> LOGGER.warn("Invalid {} from {}; using default Flux settings: {}", FILE, pack, error))
                    .orElse(FluxSettings.DEFAULT);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {} from {}; using default Flux settings", FILE, pack, e);
            return FluxSettings.DEFAULT;
        }
    }

    @Override
    protected void apply(FluxSettings settings, ResourceManager manager, ProfilerFiller profiler) {
        flux.updateSettings(settings);
    }
}
