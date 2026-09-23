package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.rune.RuneSettings;

/** Writes {@code data/thaumory/thaumory/rune.json}. */
final class RuneSettingsProvider extends FabricCodecDataProvider<RuneSettings> {
    RuneSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory", RuneSettings.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, RuneSettings> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("rune"), RuneSettings.DEFAULT);
    }

    @Override
    public String getName() {
        return "Thaumory rune settings";
    }
}
