package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.item.ThaumoryMaterials;

/** Writes {@code assets/thaumory/equipment/*.json}: how the arcane metals' armor and the monocle look when worn. */
final class EquipmentAssetProvider extends FabricCodecDataProvider<EquipmentClientInfo> {
    EquipmentAssetProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.RESOURCE_PACK, "equipment", EquipmentClientInfo.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, EquipmentClientInfo> output, HolderLookup.Provider registries) {
        humanoid(output, ThaumoryMaterials.ARCANE_IRON_ASSET);
        humanoid(output, ThaumoryMaterials.AETHER_SILVER_ASSET);
        // Only on the head of grown humanoids: textures/entity/equipment/humanoid/monocle.png.
        output.accept(ThaumoryItems.MONOCLE_ASSET.identifier(), EquipmentClientInfo.builder()
                .addLayers(EquipmentClientInfo.LayerType.HUMANOID, EquipmentClientInfo.Layer.leatherDyeable(ThaumoryItems.MONOCLE_ASSET.identifier(), false))
                .build());
    }

    /** The body and leggings layers, from {@code textures/entity/equipment/humanoid[_leggings]/<name>.png}. */
    private static void humanoid(BiConsumer<Identifier, EquipmentClientInfo> output, ResourceKey<EquipmentAsset> asset) {
        output.accept(asset.identifier(), EquipmentClientInfo.builder().addHumanoidLayers(asset.identifier()).build());
    }

    @Override
    public String getName() {
        return "Thaumory equipment assets";
    }
}
