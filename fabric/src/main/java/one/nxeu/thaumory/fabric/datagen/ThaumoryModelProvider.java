package one.nxeu.thaumory.fabric.datagen;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import java.util.List;
import java.util.Map;
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
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.block.crucible.CrucibleBlock;
import one.nxeu.thaumory.block.jar.JarBlock;
import one.nxeu.thaumory.block.pipe.EssentiaPipeBlock;
import one.nxeu.thaumory.block.pipe.FilterPipeBlock;
import one.nxeu.thaumory.block.pipe.PumpBlock;
import one.nxeu.thaumory.block.pipe.ValveBlock;
import one.nxeu.thaumory.circle.CirclePlane;
import one.nxeu.thaumory.client.RuneTint;
import one.nxeu.thaumory.item.EquipmentSet;
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

    private static final TextureSlot PIPE = TextureSlot.create("pipe");
    private static final TextureSlot ARM = TextureSlot.create("arm");
    private static final ModelTemplate PIPE_CENTER = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_center")),
            Optional.of("_center"), PIPE);
    private static final ModelTemplate PIPE_ARM = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_arm")),
            Optional.of("_arm"), PIPE, ARM);
    private static final ModelTemplate PIPE_ITEM = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_item")),
            Optional.of("_inventory"), PIPE, ARM);
    private static final TextureSlot BAND = TextureSlot.create("band");
    private static final ModelTemplate PEDESTAL = new ModelTemplate(Optional.of(Thaumory.id("block/template_pedestal")),
            Optional.empty(), TextureSlot.SIDE, TextureSlot.TOP, BAND);
    private static final ModelTemplate FILTER_PIPE_CENTER = new ModelTemplate(Optional.of(Thaumory.id("block/template_pipe_filter_center")),
            Optional.of("_center"), PIPE, BAND);

    /** Cauldron shapes with Thaumory's own textures. Water is a plain texture, so it needs no tint. */
    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        generators.createTrivialCube(ThaumoryBlocks.POLLUTED_SOIL.get());
        generators.createAmethystCluster(ThaumoryBlocks.ARCANE_CRYSTAL.get());
        generators.registerSimpleFlatItemModel(ThaumoryBlocks.ARCANE_CRYSTAL.get());
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

        // Only the centre mark is a model; the rings turn, so CircleCoreRenderer draws them.
        CircleCoreBlock core = ThaumoryBlocks.CIRCLE_CORE.get();
        Identifier coreModel = CHALK_PATTERN.create(core,
                new TextureMapping().put(PATTERN, TextureMapping.getBlockTexture(core, "_center")), generators.modelOutput);
        // A built-in pedestal stands over the mark as a slim altar.
        Identifier pedestalModel = PEDESTAL.createWithSuffix(core, "_pedestal", new TextureMapping()
                .put(TextureSlot.SIDE, TextureMapping.getBlockTexture(core, "_pedestal_side"))
                .put(TextureSlot.TOP, TextureMapping.getBlockTexture(core, "_pedestal_top"))
                .put(BAND, TextureMapping.getBlockTexture(core, "_pedestal_band")), generators.modelOutput);
        MultiPartGenerator coreParts = MultiPartGenerator.multiPart(core);
        for (Direction front : Direction.values()) {
            coreParts.with(new ConditionBuilder().term(CircleCoreBlock.FACING, front),
                    BlockModelGenerators.plainVariant(coreModel).with(onFace(front, Quadrant.R0)));
        }
        generators.blockStateOutput.accept(coreParts
                .with(new ConditionBuilder().term(CircleCoreBlock.PEDESTAL, true), BlockModelGenerators.plainVariant(pedestalModel)));
        generators.itemModelOutput.accept(ThaumoryItems.PEDESTAL.get(), ItemModelUtils.plainModel(pedestalModel));

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

    /**
     * A mark in the middle, and an arm towards each joined neighbor (the arm texture points north),
     * the floor models turned onto the face the pattern is drawn on.
     */
    private static void chalkPattern(BlockModelGenerators generators, ChalkPatternBlock pattern) {
        Identifier mark = CHALK_PATTERN.createWithSuffix(pattern, "_mark",
                new TextureMapping().put(PATTERN, TextureMapping.getBlockTexture(pattern, "_mark")), generators.modelOutput);
        Identifier arm = CHALK_PATTERN.createWithSuffix(pattern, "_arm",
                new TextureMapping().put(PATTERN, TextureMapping.getBlockTexture(pattern, "_arm")), generators.modelOutput);
        // In a fixed order, so the generated file stays the same from run to run.
        List<Map.Entry<Direction, Quadrant>> arms = List.of(Map.entry(Direction.NORTH, Quadrant.R0), Map.entry(Direction.EAST, Quadrant.R90),
                Map.entry(Direction.SOUTH, Quadrant.R180), Map.entry(Direction.WEST, Quadrant.R270));
        MultiPartGenerator parts = MultiPartGenerator.multiPart(pattern);
        for (Direction front : Direction.values()) {
            parts.with(new ConditionBuilder().term(ChalkPatternBlock.FACING, front),
                    BlockModelGenerators.plainVariant(mark).with(onFace(front, Quadrant.R0)));
            for (Map.Entry<Direction, Quadrant> side : arms) {
                parts.with(new ConditionBuilder().term(ChalkPatternBlock.FACING, front)
                                .term(ChalkPatternBlock.CONNECTIONS.get(side.getKey()), true),
                        BlockModelGenerators.plainVariant(arm).with(onFace(front, side.getValue())));
            }
        }
        generators.blockStateOutput.accept(parts);
    }

    /**
     * Turns a floor model by {@code turn} about the vertical, then onto the face {@code front} points
     * away from ({@link CirclePlane}), as the one x, y and z rotation that does both.
     */
    private static VariantMutator onFace(Direction front, Quadrant turn) {
        OctahedralGroup wanted = CirclePlane.rotation(front).compose(turn.rotationY);
        for (Quadrant x : Quadrant.values()) {
            for (Quadrant y : Quadrant.values()) {
                for (Quadrant z : Quadrant.values()) {
                    if (Quadrant.fromXYZAngles(x, y, z) == wanted) {
                        return VariantMutator.X_ROT.withValue(x).then(VariantMutator.Y_ROT.withValue(y)).then(VariantMutator.Z_ROT.withValue(z));
                    }
                }
            }
        }
        throw new IllegalStateException("No rotation for " + front + " " + turn);
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        generators.generateFlatItem(ThaumoryItems.ARCANE_LOUPE.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generators.generateFlatItem(ThaumoryItems.MONOCLE.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.WAND.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generators.generateFlatItem(ThaumoryItems.ARCANE_CODEX.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.BLANK_RUNE.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.CIRCLE_CORE.get(), ModelTemplates.FLAT_ITEM);
        // The blank rune with a glyph over it in the rune's aspect color.
        RuneItem rune = ThaumoryItems.RUNE.get();
        Identifier runeModel = ModelTemplates.TWO_LAYERED_ITEM.create(rune, TextureMapping.layered(
                TextureMapping.getItemTexture(ThaumoryItems.BLANK_RUNE.get()), TextureMapping.getItemTexture(rune, "_glyph")),
                generators.modelOutput);
        generators.itemModelOutput.accept(rune, ItemModelUtils.tintedModel(runeModel,
                ItemModelUtils.constantTint(-1), new RuneTint(0xFFFFFF)));
        generators.generateFlatItem(ThaumoryItems.LABEL.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.TRANSCRIPT.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.BLANK_SCROLL.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.SCROLL.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThaumoryItems.AMULET.get(), ModelTemplates.FLAT_ITEM);
        for (var chalk : List.of(ThaumoryItems.CHALK, ThaumoryItems.AMPLIFYING_CHALK, ThaumoryItems.EXTENDING_CHALK,
                ThaumoryItems.ECONOMIZING_CHALK, ThaumoryItems.STABILIZING_CHALK)) {
            generators.generateFlatItem(chalk.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        }
        for (EquipmentSet set : List.of(ThaumoryItems.ARCANE_IRON, ThaumoryItems.AETHER_SILVER)) {
            generators.generateFlatItem(set.ingot().get(), ModelTemplates.FLAT_ITEM);
            set.tools().forEach(tool -> generators.generateFlatItem(tool.get(), ModelTemplates.FLAT_HANDHELD_ITEM));
            generators.generateTrimmableArmorSet(set.helmet().get(), set.chestplate().get(), set.leggings().get(), set.boots().get(),
                    false, Map.of());
        }
    }
}
