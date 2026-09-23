package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.pipe.PipeSettings;

/** Writes {@code data/thaumory/thaumory/pipe.json}. */
final class PipeSettingsProvider extends FabricCodecDataProvider<PipeSettings> {
    PipeSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory", PipeSettings.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, PipeSettings> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("pipe"), PipeSettings.DEFAULT);
    }

    @Override
    public String getName() {
        return "Thaumory pipe settings";
    }
}
