package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.block.core.CoreBlock;
import one.nxeu.thaumory.block.crucible.CrucibleBlock;
import one.nxeu.thaumory.block.jar.JarBlock;
import one.nxeu.thaumory.block.pedestal.PedestalBlock;
import one.nxeu.thaumory.block.pipe.EssentiaPipeBlock;
import one.nxeu.thaumory.block.pipe.FilterPipeBlock;
import one.nxeu.thaumory.block.pipe.PumpBlock;
import one.nxeu.thaumory.block.pipe.ValveBlock;
import one.nxeu.thaumory.client.RuneTint;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryItems;

final class ThaumoryModelProvider extends FabricModelProvider {
    ThaumoryModelProvider(FabricPackOutput output) {
        super(output);
    }

    private static final ModelTemplate CAULDRON = new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/cauldron")),
            Optional.empty(), TextureSlot.PARTICLE, TextureSlot.TOP, TextureSlot.BOTTOM, TextureSlot.SIDE, TextureSlot.INSIDE);

    private static final TextureSlot LID = TextureSlot.create("lid");
    private static final TextureSlot LABEL = TextureSlot.create("label");
    private static final ModelTemplate JAR = new ModelTemplate(Optional.of(Thaumory.id("block/template_jar")),
            Optional.empty(), TextureSlot.SIDE, TextureSlot.BOTTOM, LID);
    private static final ModelTemplate JAR_LABELED = new ModelTemplate(Optional.of(Thaumory.id("block/template_jar_labeled")),
            Optional.empty(), TextureSlot.SIDE, TextureSlot.BOTTOM, LID, LABEL);

    private static final TextureSlot PATTERN = TextureSlot.create("pattern");
    private static final ModelTemplate CHALK_PATTERN = new ModelTemplate(Optional.of(Thaumory.id("block/template_chalk_pattern")),
            Optional.empty(), PATTERN);

    private static final ModelTemplate PEDESTAL = new ModelTemplate(Optional.of(Thaumory.id("block/template_pedestal")),
            Optional.empty(), TextureSlot.SIDE, TextureSlot.TOP);

    private static final TextureSlot PIPE = TextureSlot.create("pipe");
    private static final TextureSlot ARM = TextureSlot.create("arm");
    private static final ModelTemplate PIPE_CENTER = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_center")),
            Optional.of("_center"), PIPE);
    private static final ModelTemplate PIPE_ARM = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_arm")),
            Optional.of("_arm"), PIPE, ARM);
    private static final ModelTemplate PIPE_ITEM = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_item")),
            Optional.of("_inventory"), PIPE, ARM);
    private static final TextureSlot BAND = TextureSlot.create("band");
    private static final ModelTemplate FILTER_PIPE_CENTER = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_filter_center")),
            Optional.of("_center"), PIPE, BAND);

    /** Cauldron shapes with Thaumory's own textures. Water is a plain texture, so it needs no tint. */
    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        generators.createTrivialCube(ThaumoryBlocks.POLLUTED_SOIL.get());
        generators.createTrivialCube(ThaumoryBlocks.POLLUTED_STONE.get());
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

        JarBlock jar = ThaumoryBlocks.JAR.get();
        TextureMapping jarTextures = new TextureMapping()
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(jar, "_side"))
                .put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(jar, "_bottom"))
                .put(LID, TextureMapping.getBlockTexture(jar, "_lid"));
        Identifier plain = JAR.create(jar, jarTextures, generators.modelOutput);
        Identifier labeled = JAR_LABELED.createWithSuffix(jar, "_labeled",
                jarTextures.copyAndUpdate(LABEL, TextureMapping.getBlockTexture(jar, "_label")), generators.modelOutput);
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(jar).with(PropertyDispatch.initial(JarBlock.LABELED)
                .select(false, BlockModelGenerators.plainVariant(plain))
                .select(true, BlockModelGenerators.plainVariant(labeled))));
        generators.registerSimpleItemModel(jar, plain);

        // Only the centre mark is a model; the rings turn, so CoreRenderer draws them.
        CoreBlock core = ThaumoryBlocks.CORE.get();
        Identifier coreModel = CHALK_PATTERN.create(core,
                new TextureMapping().put(PATTERN, TextureMapping.getBlockTexture(core, "_center")), generators.modelOutput);
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(core, BlockModelGenerators.plainVariant(coreModel)));

        PedestalBlock pedestal = ThaumoryBlocks.PEDESTAL.get();
        Identifier pedestalModel = PEDESTAL.create(pedestal, new TextureMapping()
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(pedestal, "_side"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(pedestal, "_top")), generators.modelOutput);
        generators.blockStateOutput.accept(MultiVariantGenerator.dispatch(pedestal, BlockModelGenerators.plainVariant(pedestalModel)));
        generators.registerSimpleItemModel(pedestal, pedestalModel);

        EssentiaPipeBlock pipe = ThaumoryBlocks.PIPE.get();
        TextureMapping pipeTextures = pipeTextures(TextureMapping.getBlockTexture(pipe));
        MultiVariant arm = BlockModelGenerators.plainVariant(PIPE_ARM.create(pipe, pipeTextures, generators.modelOutput));
        pipe(generators, pipe, arm, BlockModelGenerators.plainVariant(PIPE_CENTER.create(pipe, pipeTextures, generators.modelOutput)),
                TextureMapping.getBlockTexture(pipe));
        // The band takes the filter's color, so it is a tinted layer over the plain joint.
        FilterPipeBlock filter = ThaumoryBlocks.FILTER_PIPE.get();
        pipe(generators, filter, arm, BlockModelGenerators.plainVariant(FILTER_PIPE_CENTER.create(filter,
                pipeTextures.copyAndUpdate(BAND, TextureMapping.getBlockTexture(filter, "_band")), generators.modelOutput)),
                TextureMapping.getBlockTexture(filter));
        PumpBlock pump = ThaumoryBlocks.PUMP.get();
        pipe(generators, pump, arm, BlockModelGenerators.plainVariant(PIPE_CENTER.create(pump,
                pipeTextures(TextureMapping.getBlockTexture(pump)), generators.modelOutput)), TextureMapping.getBlockTexture(pump));
        ValveBlock valve = ThaumoryBlocks.VALVE.get();
        Identifier open = PIPE_CENTER.create(valve, pipeTextures(TextureMapping.getBlockTexture(valve)), generators.modelOutput);
        Identifier closed = PIPE_CENTER.createWithSuffix(valve, "_center_closed", pipeTextures(TextureMapping.getBlockTexture(valve, "_closed")),
                generators.modelOutput);
        MultiPartGenerator valveParts = pipeParts(valve, arm)
                .with(new ConditionBuilder().term(ValveBlock.POWERED, false), BlockModelGenerators.plainVariant(open))
                .with(new ConditionBuilder().term(ValveBlock.POWERED, true), BlockModelGenerators.plainVariant(closed));
        generators.blockStateOutput.accept(valveParts);
        generators.registerSimpleItemModel(valve, PIPE_ITEM.create(valve, pipeTextures(TextureMapping.getBlockTexture(valve)), generators.modelOutput));

        for (var pattern : List.of(ThaumoryBlocks.CHALK_LINE, ThaumoryBlocks.AMPLIFYING_PATTERN, ThaumoryBlocks.EXTENDING_PATTERN,
                ThaumoryBlocks.ECONOMIZING_PATTERN, ThaumoryBlocks.STABILIZING_PATTERN)) {
            chalkPattern(generators, pattern.get());
        }
    }

    /** A pipe's joint texture, with the plain pipe's arms. */
    private static TextureMapping pipeTextures(Material joint) {
        return new TextureMapping().put(PIPE, joint).put(ARM, TextureMapping.getBlockTexture(ThaumoryBlocks.PIPE.get(), "_arm"));
    }

    /** A joint in the middle, and an arm towards each joined side (the arm model points north). */
    private static void pipe(BlockModelGenerators generators, EssentiaPipeBlock pipe, MultiVariant arm, MultiVariant center,
            Material itemJoint) {
        generators.blockStateOutput.accept(pipeParts(pipe, arm).with(center));
        generators.registerSimpleItemModel(pipe, PIPE_ITEM.create(pipe, pipeTextures(itemJoint), generators.modelOutput));
    }

    private static MultiPartGenerator pipeParts(EssentiaPipeBlock pipe, MultiVariant arm) {
        return MultiPartGenerator.multiPart(pipe)
                .with(new ConditionBuilder().term(BlockStateProperties.NORTH, true), arm)
                .with(new ConditionBuilder().term(BlockStateProperties.EAST, true), arm.with(BlockModelGenerators.Y_ROT_90))
                .with(new ConditionBuilder().term(BlockStateProperties.SOUTH, true), arm.with(BlockModelGenerators.Y_ROT_180))
                .with(new ConditionBuilder().term(BlockStateProperties.WEST, true), arm.with(BlockModelGenerators.Y_ROT_270))
                .with(new ConditionBuilder().term(BlockStateProperties.UP, true), arm.with(BlockModelGenerators.X_ROT_270))
                .with(new ConditionBuilder().term(BlockStateProperties.DOWN, true), arm.with(BlockModelGenerators.X_ROT_90));
    }

    /** A mark in the middle, and an arm towards each joined neighbor (the arm texture points north). */
    private static void chalkPattern(BlockModelGenerators generators, ChalkPatternBlock pattern) {
        Identifier mark = CHALK_PATTERN.createWithSuffix(pattern, "_mark",
                new TextureMapping().put(PATTERN, TextureMapping.getBlockTexture(pattern, "_mark")), generators.modelOutput);
        Identifier arm = CHALK_PATTERN.createWithSuffix(pattern, "_arm",
                new TextureMapping().put(PATTERN, TextureMapping.getBlockTexture(pattern, "_arm")), generators.modelOutput);
        MultiVariant armVariant = BlockModelGenerators.plainVariant(arm);
        generators.blockStateOutput.accept(MultiPartGenerator.multiPart(pattern)
                .with(BlockModelGenerators.plainVariant(mark))
                .with(new ConditionBuilder().term(BlockStateProperties.NORTH, true), armVariant)
                .with(new ConditionBuilder().term(BlockStateProperties.EAST, true), armVariant.with(BlockModelGenerators.Y_ROT_90))
                .with(new ConditionBuilder().term(BlockStateProperties.SOUTH, true), armVariant.with(BlockModelGenerators.Y_ROT_180))
                .with(new ConditionBuilder().term(BlockStateProperties.WEST, true), armVariant.with(BlockModelGenerators.Y_ROT_270)));
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        generators.generateFlatItem(ThaumoryItems.ARCANE_LOUPE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generators.generateFlatItem(ThaumoryItems.WAND.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generators.generateFlatItem(ThaumoryItems.ARCANE_CODEX.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.BLANK_RUNE.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.CORE.get(), ModelTemplates.FLAT_ITEM);
        // The blank rune with a glyph over it in the rune's aspect color.
        RuneItem rune = ThaumoryItems.RUNE.get();
        Identifier runeModel = ModelTemplates.TWO_LAYERED_ITEM.create(rune, TextureMapping.layered(
                TextureMapping.getItemTexture(ThaumoryItems.BLANK_RUNE.get()), TextureMapping.getItemTexture(rune, "_glyph")),
                generators.modelOutput);
        generators.itemModelOutput.accept(rune, ItemModelUtils.tintedModel(runeModel,
                ItemModelUtils.constantTint(-1), new RuneTint(0xFFFFFF)));
        generators.generateFlatItem(ThaumoryItems.LABEL.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.TRANSCRIPT.get(), ModelTemplates.FLAT_ITEM);
        for (var chalk : List.of(ThaumoryItems.CHALK, ThaumoryItems.AMPLIFYING_CHALK, ThaumoryItems.EXTENDING_CHALK,
                ThaumoryItems.ECONOMIZING_CHALK, ThaumoryItems.STABILIZING_CHALK)) {
            generators.generateFlatItem(chalk.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        }
    }
}
