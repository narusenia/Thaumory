package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;

/** Writes {@code data/thaumory/thaumory/item_aspects/vanilla.json}. */
final class ItemAspectProvider extends FabricCodecDataProvider<ItemAspectFile> {
    ItemAspectProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/item_aspects", ItemAspectFile.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, ItemAspectFile> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("vanilla"), VanillaItemAspects.build());
    }

    @Override
    public String getName() {
        return "Thaumory item aspects";
    }
}
