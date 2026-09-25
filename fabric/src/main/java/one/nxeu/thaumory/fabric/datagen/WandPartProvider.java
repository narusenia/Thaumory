package one.nxeu.thaumory.fabric.datagen;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.wand.WandPart;

/**
 * Writes {@code data/thaumory/thaumory/wand_part/*.json} (requirements §7.1): the caps, each holding
 * twice the last, and the cores, the crystal one stronger and digging a tier deeper.
 */
final class WandPartProvider extends FabricCodecDataProvider<WandPart> {
    WandPartProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/wand_part", WandPart.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, WandPart> output, HolderLookup.Provider registries) {
        cap(output, ThaumoryItems.GOLD_WAND_CAP.get(), 16);
        cap(output, ThaumoryItems.ARCANE_IRON_WAND_CAP.get(), 32);
        cap(output, ThaumoryItems.AETHER_SILVER_WAND_CAP.get(), 64);
        core(output, Thaumory.id("wood_wand_core"), Items.STICK, 1.0, BlockTags.INCORRECT_FOR_STONE_TOOL);
        core(output, Thaumory.id("crystal_wand_core"), ThaumoryItems.CRYSTAL_WAND_CORE.get(), 1.25, BlockTags.INCORRECT_FOR_IRON_TOOL);
    }

    private static void cap(BiConsumer<Identifier, WandPart> output, Item item, int essentia) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        output.accept(id, new WandPart(id, Optional.of(new WandPart.Cap(essentia)), Optional.empty()));
    }

    private static void core(BiConsumer<Identifier, WandPart> output, Identifier file, Item item, double power, TagKey<Block> incorrectFor) {
        output.accept(file, new WandPart(BuiltInRegistries.ITEM.getKey(item), Optional.empty(),
                Optional.of(new WandPart.Core(power, incorrectFor.location()))));
    }

    @Override
    public String getName() {
        return "Thaumory wand parts";
    }
}
