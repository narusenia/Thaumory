package one.nxeu.thaumory.fabric.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CircleScan;

/** The ruins of old circles (requirements §17.3). */
public class OldCircleGameTests {
    /** Where the template's corner goes; its floor is then at relative y 1. */
    private static final BlockPos ORIGIN = new BlockPos(0, 1, 0);
    private static final BlockPos CORE = ORIGIN.offset(3, 1, 3);

    @GameTest(maxTicks = 20)
    public void redrawingTheBrokenRingMakesTheCircleWork(GameTestHelper helper) {
        // The ground the ruin is set into: the floor replaces its top, and the outer ring runs out onto it.
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(6, 0, 6))) {
            helper.setBlock(pos, Blocks.DIRT);
        }
        StructureTemplate template = helper.getLevel().getServer().getStructureTemplateManager()
                .getOrCreate(Thaumory.id("old_circle/light"));
        template.placeInWorld(helper.getLevel(), helper.absolutePos(ORIGIN), helper.absolutePos(ORIGIN), new StructurePlaceSettings(),
                helper.getLevel().getRandom(), Block.UPDATE_ALL);

        CircleCoreBlockEntity core = helper.getBlockEntity(CORE, CircleCoreBlockEntity.class);
        helper.assertValueEqual(core.runes(), List.of(ThaumoryAspects.LUX.id(), ThaumoryAspects.IGNIS.id()), "runes");
        helper.assertTrue(core.essentia().isEmpty(), "the Core should be empty");
        core.rescan();
        helper.assertValueEqual(core.scan().rings(), 0, "rings before repair");
        // What is left of the chalk lies on the two round rings, the outer one broken in places.
        int outer = 0;
        for (BlockPos pos : BlockPos.betweenClosed(CORE.offset(-3, 0, -3), CORE.offset(3, 0, 3))) {
            if (helper.getBlockState(pos).is(ThaumoryBlocks.CHALK_LINE.get())) {
                int dx = pos.getX() - CORE.getX();
                int dz = pos.getZ() - CORE.getZ();
                helper.assertTrue(CircleScan.onRing(1, dx, dz) || CircleScan.onRing(2, dx, dz), "chalk off the rings at " + dx + ", " + dz);
                outer += CircleScan.onRing(2, dx, dz) ? 1 : 0;
            }
        }
        helper.assertTrue(outer >= 8 && outer < CircleScan.cells(2).size(), "outer ring has " + outer + " cells");

        helper.assertTrue(helper.getBlockEntity(ORIGIN.offset(6, 1, 6), ChestBlockEntity.class).getLootTable() != null, "chest has no loot");

        helper.setBlock(CORE.offset(1, 0, -1), ThaumoryBlocks.CHALK_LINE.get());
        helper.setBlock(CORE.offset(-1, 0, 1), ThaumoryBlocks.CHALK_LINE.get());
        core.rescan();
        helper.assertValueEqual(core.scan().rings(), 1, "rings after repair");
        helper.assertTrue(core.effectId().isPresent(), "the repaired circle is not a known combination");
        helper.succeed();
    }
}
