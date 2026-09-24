package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;
import one.nxeu.thaumory.infusion.CapacityFile;
import one.nxeu.thaumory.item.ThaumoryMaterials;

/** Writes {@code data/thaumory/thaumory/infusion_capacity/items.json}: the arcane metals', blank scrolls' and amulets' capacity (requirements §10.2). */
final class InfusionCapacityProvider extends FabricCodecDataProvider<CapacityFile> {
    InfusionCapacityProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/infusion_capacity", CapacityFile.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, CapacityFile> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("items"), new CapacityFile(List.of(
                tag(ThaumoryMaterials.ARCANE_IRON_EQUIPMENT, 3),
                tag(ThaumoryMaterials.AETHER_SILVER_EQUIPMENT, 5),
                // Room for any one effect; once infused it is a scroll, which has none.
                new CapacityFile.Entry(new ItemAspectFile.Target(Thaumory.id("blank_scroll"), false), 3),
                // Less than gear: it works from anywhere in the inventory.
                new CapacityFile.Entry(new ItemAspectFile.Target(Thaumory.id("amulet"), false), 2))));
    }

    private static CapacityFile.Entry tag(TagKey<Item> tag, int capacity) {
        return new CapacityFile.Entry(new ItemAspectFile.Target(tag.location(), true), capacity);
    }

    @Override
    public String getName() {
        return "Thaumory infusion capacity";
    }
}
