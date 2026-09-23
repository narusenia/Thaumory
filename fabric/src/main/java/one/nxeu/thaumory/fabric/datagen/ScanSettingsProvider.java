package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.scan.ScanSettings;

/** Writes {@code data/thaumory/thaumory/scanning.json}. */
final class ScanSettingsProvider extends FabricCodecDataProvider<ScanSettings> {
    ScanSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory", ScanSettings.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, ScanSettings> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("scanning"), ScanSettings.DEFAULT);
    }

    @Override
    public String getName() {
        return "Thaumory scan settings";
    }
}
