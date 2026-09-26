package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.pouch.PouchSettings;

/** Writes {@code data/thaumory/thaumory/pouch.json}. */
final class PouchSettingsProvider extends FabricCodecDataProvider<PouchSettings> {
    PouchSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory", PouchSettings.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, PouchSettings> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("pouch"), PouchSettings.DEFAULT);
    }

    @Override
    public String getName() {
        return "Thaumory pouch settings";
    }
}
