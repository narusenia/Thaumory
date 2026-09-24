package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.item.ThaumoryItems;

final class ThaumoryItemTagProvider extends FabricTagsProvider<Item> {
    ThaumoryItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.ITEM, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // The tools that act on a circle core; they must not end up on its pedestal.
        var ignored = builder(ThaumoryItems.PEDESTAL_IGNORED);
        for (var item : List.of(ThaumoryItems.WAND, ThaumoryItems.JAR, ThaumoryItems.ARCANE_LOUPE, ThaumoryItems.ARCANE_CODEX,
                ThaumoryItems.RUNE, ThaumoryItems.PEDESTAL, ThaumoryItems.CHALK, ThaumoryItems.AMPLIFYING_CHALK, ThaumoryItems.EXTENDING_CHALK,
                ThaumoryItems.ECONOMIZING_CHALK, ThaumoryItems.STABILIZING_CHALK)) {
            ignored.add(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item.get())));
        }
    }
}
