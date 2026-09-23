package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.flux.FluxSettings;

/** Writes {@code data/thaumory/thaumory/flux.json}. */
final class FluxSettingsProvider extends FabricCodecDataProvider<FluxSettings> {
    FluxSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory", FluxSettings.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, FluxSettings> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("flux"), FluxSettings.DEFAULT);
    }

    @Override
    public String getName() {
        return "Thaumory flux settings";
    }
}
