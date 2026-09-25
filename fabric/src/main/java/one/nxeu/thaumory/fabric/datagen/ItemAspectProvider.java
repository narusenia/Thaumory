package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;
import one.nxeu.thaumory.item.ThaumoryItems;

/** Writes {@code data/thaumory/thaumory/item_aspects/vanilla.json} and {@code thaumory.json} (Thaumory's own items). */
final class ItemAspectProvider extends FabricCodecDataProvider<ItemAspectFile> {
    ItemAspectProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/item_aspects", ItemAspectFile.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, ItemAspectFile> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("vanilla"), VanillaItemAspects.build());
        // A rune's aspect lives in a component, so the item itself has none (requirements §4.2).
        output.accept(Thaumory.id("thaumory"), new ItemAspectFileBuilder().none(ThaumoryItems.RUNE.get())
                .item(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get(), new AspectStack(ThaumoryAspects.ARCANUM, 3), new AspectStack(ThaumoryAspects.AURORA, 2))
                .item(ThaumoryItems.ARCANE_CRYSTAL.get(), new AspectStack(ThaumoryAspects.ARCANUM, 6), new AspectStack(ThaumoryAspects.AURORA, 4))
                .item(ThaumoryItems.POLLUTED_SOIL.get(), new AspectStack(ThaumoryAspects.TERRA, 4), new AspectStack(ThaumoryAspects.SORDES, 2))
                .item(ThaumoryItems.POLLUTED_STONE.get(), new AspectStack(ThaumoryAspects.TERRA, 4), new AspectStack(ThaumoryAspects.SORDES, 2))
                .item(ThaumoryItems.FLUX_CRYSTAL.get(), new AspectStack(ThaumoryAspects.SORDES, 4))
                .build());
    }

    @Override
    public String getName() {
        return "Thaumory item aspects";
    }
}
