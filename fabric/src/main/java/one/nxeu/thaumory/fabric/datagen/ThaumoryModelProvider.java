package one.nxeu.thaumory.fabric.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import java.util.Optional;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.crucible.CrucibleBlock;
import one.nxeu.thaumory.item.ThaumoryItems;

final class ThaumoryModelProvider extends FabricModelProvider {
    ThaumoryModelProvider(FabricPackOutput output) {
        super(output);
    }

    private static final ModelTemplate CAULDRON = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/cauldron")),
            Optional.empty(), TextureSlot.PARTICLE, TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.INSIDE);

    /** Cauldron shapes with Thaumory's own textures. Water is a plain texture, so it needs no tint. */
    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        CrucibleBlock crucible = ThaumoryBlocks.CRUCIBLE.get();
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(crucible, "_side"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(crucible, "_top"))
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(crucible, "_bottom"))
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(crucible, "_side"))
                .put(TextureSlot.INSIDE, TextureMapping.getBlockTexture(crucible, "_inner"));
        TextureMapping filled = textures.copyAndUpdate(TextureSlot.CONTENT, TextureMapping.getBlockTexture(crucible, "_water"));

        Identifier empty = CAULDRON.create(crucible, textures, generators.modelOutput);
        Identifier level1 = ModelTemplates.CAULDRON_LEVEL1.createWithSuffix(crucible, "_level1", filled, generators.modelOutput);
        Identifier level2 = ModelTemplates.CAULDRON_LEVEL2.createWithSuffix(crucible, "_level2", filled, generators.modelOutput);
        Identifier full = ModelTemplates.CAULDRON_FULL.createWithSuffix(crucible, "_full", filled, generators.modelOutput);

        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(crucible).with(PropertyDispatch.initial(CrucibleBlock.LEVEL)
                .select(0, BlockModelGenerators.plainVariant(empty))
                .select(1, BlockModelGenerators.plainVariant(level1))
                .select(2, BlockModelGenerators.plainVariant(level2))
                .select(3, BlockModelGenerators.plainVariant(full))));
        generators.registerSimpleItemModel(crucible, empty);
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        generators.generateFlatItem(ThaumoryItems.ARCANE_LOUPE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generators.generateFlatItem(ThaumoryItems.ARCANE_CODEX.get(), ModelTemplates.FLAT_ITEM);
    }
}
