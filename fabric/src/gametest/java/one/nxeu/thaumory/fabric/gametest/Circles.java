package one.nxeu.thaumory.fabric.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CirclePlane;

/** Builds one-ring circles for GameTests: stone underfoot, a Core, its runes and a full store of Essentia. */
final class Circles {
    private Circles() {}

    /** Stone across the whole test area at relative y 1, for things to stand on around a Core at y 2. */
    static void floor(GameTestHelper helper) {
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    /** A Core at {@code core} (relative) inside a ring of plain chalk, holding {@code runes} in slot order. */
    static CircleCoreBlockEntity build(GameTestHelper helper, BlockPos core, Aspect... runes) {
        return build(helper, core, Direction.UP, runes);
    }

    /** The same on the face {@code front} points away from: stone behind, and the ring on that face. */
    static CircleCoreBlockEntity build(GameTestHelper helper, BlockPos core, Direction front, Aspect... runes) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos cell = core.offset(CirclePlane.offset(front, dx, dz));
                helper.setBlock(cell.relative(front.getOpposite()), Blocks.STONE);
                if (dx != 0 || dz != 0) {
                    helper.setBlock(cell, ThaumoryBlocks.CHALK_LINE.get().defaultBlockState().setValue(ChalkPatternBlock.FACING, front));
                }
            }
        }
        helper.setBlock(core, ThaumoryBlocks.CIRCLE_CORE.get().defaultBlockState().setValue(ChalkPatternBlock.FACING, front));
        CircleCoreBlockEntity entity = helper.getBlockEntity(core, CircleCoreBlockEntity.class);
        AspectList.Builder essentia = AspectList.builder();
        for (Aspect rune : runes) {
            entity.insert(rune.id());
            essentia.add(rune, CircleCoreBlockEntity.capacity());
        }
        entity.setEssentia(essentia.build());
        entity.rescan();
        helper.assertValueEqual(entity.scan().rings(), 1, "rings");
        return entity;
    }
}
