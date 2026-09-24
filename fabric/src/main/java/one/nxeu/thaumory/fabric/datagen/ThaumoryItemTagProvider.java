package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.item.EquipmentSet;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.item.ThaumoryMaterials;

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
            ignored.add(key(item.get()));
        }

        builder(ThaumoryMaterials.REPAIRS_ARCANE_IRON).add(key(ThaumoryItems.ARCANE_IRON.ingot().get()));
        builder(ThaumoryMaterials.REPAIRS_AETHER_SILVER).add(key(ThaumoryItems.AETHER_SILVER.ingot().get()));
        equipment(ThaumoryMaterials.ARCANE_IRON_EQUIPMENT, ThaumoryItems.ARCANE_IRON);
        equipment(ThaumoryMaterials.AETHER_SILVER_EQUIPMENT, ThaumoryItems.AETHER_SILVER);
        // Vanilla's tags carry enchantability, trims and the like over to the gear.
        for (EquipmentSet set : List.of(ThaumoryItems.ARCANE_IRON, ThaumoryItems.AETHER_SILVER)) {
            builder(ItemTags.SWORDS).add(key(set.sword().get()));
            builder(ItemTags.PICKAXES).add(key(set.pickaxe().get()));
            builder(ItemTags.CLUSTER_MAX_HARVESTABLES).add(key(set.pickaxe().get()));
            builder(ItemTags.AXES).add(key(set.axe().get()));
            builder(ItemTags.SHOVELS).add(key(set.shovel().get()));
            builder(ItemTags.HOES).add(key(set.hoe().get()));
            builder(ItemTags.HEAD_ARMOR).add(key(set.helmet().get()));
            builder(ItemTags.CHEST_ARMOR).add(key(set.chestplate().get()));
            builder(ItemTags.LEG_ARMOR).add(key(set.leggings().get()));
            builder(ItemTags.FOOT_ARMOR).add(key(set.boots().get()));
        }
    }

    private void equipment(TagKey<Item> tag, EquipmentSet set) {
        var builder = builder(tag);
        set.tools().forEach(item -> builder.add(key(item.get())));
        set.armor().forEach(item -> builder.add(key(item.get())));
    }

    private static ResourceKey<Item> key(Item item) {
        return ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item));
    }
}
