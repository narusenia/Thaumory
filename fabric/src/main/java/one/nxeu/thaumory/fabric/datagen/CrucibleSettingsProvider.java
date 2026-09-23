package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.crucible.CrucibleSettings;

/** Writes {@code data/thaumory/thaumory/crucible.json}. */
final class CrucibleSettingsProvider extends FabricCodecDataProvider<CrucibleSettings> {
    CrucibleSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory", CrucibleSettings.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, CrucibleSettings> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("crucible"), CrucibleSettings.DEFAULT);
    }

    @Override
    public String getName() {
        return "Thaumory crucible settings";
    }
}
