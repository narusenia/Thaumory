package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.jar.JarSettings;

/** Writes {@code data/thaumory/thaumory/jar.json}. */
final class JarSettingsProvider extends FabricCodecDataProvider<JarSettings> {
    JarSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory", JarSettings.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, JarSettings> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("jar"), JarSettings.DEFAULT);
    }

    @Override
    public String getName() {
        return "Thaumory jar settings";
    }
}
