package one.nxeu.thaumory.fabric.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CirclePlane;
import one.nxeu.thaumory.circle.CircleScan;

/**
 * Builds circles for GameTests, of one ring unless asked for more: stone underfoot, a Core, its runes
 * and a full store of Essentia.
 */
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
        return build(helper, core, front, ThaumoryBlocks.CIRCLE_CORE.get(), 1, runes);
    }

    /**
     * {@code block} at {@code core} inside {@code rings} round rings of plain chalk, of which it reads
     * as many as its rank lets it. The stone behind reaches as far as the outer ring: 4 × rings − 1 across.
     */
    static CircleCoreBlockEntity build(GameTestHelper helper, BlockPos core, CircleCoreBlock block, int rings, Aspect... runes) {
        return build(helper, core, Direction.UP, block, rings, runes);
    }

    private static CircleCoreBlockEntity build(GameTestHelper helper, BlockPos core, Direction front, CircleCoreBlock block, int rings,
            Aspect... runes) {
        int reach = CircleScan.radius(rings);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                helper.setBlock(core.offset(CirclePlane.offset(front, dx, dz)).relative(front.getOpposite()), Blocks.STONE);
            }
        }
        for (int ring = 1; ring <= rings; ring++) {
            for (CircleScan.Offset cell : CircleScan.cells(ring)) {
                helper.setBlock(core.offset(CirclePlane.offset(front, cell.dx(), cell.dz())),
                        ThaumoryBlocks.CHALK_LINE.get().defaultBlockState().setValue(ChalkPatternBlock.FACING, front));
            }
        }
        helper.setBlock(core, block.defaultBlockState().setValue(ChalkPatternBlock.FACING, front));
        CircleCoreBlockEntity entity = helper.getBlockEntity(core, CircleCoreBlockEntity.class);
        AspectList.Builder essentia = AspectList.builder();
        for (Aspect rune : runes) {
            entity.insert(rune.id());
            essentia.add(rune, CircleCoreBlockEntity.capacity());
        }
        entity.setEssentia(essentia.build());
        entity.rescan();
        helper.assertValueEqual(entity.scan().rings(), Math.min(rings, block.maxRings()), "rings");
        return entity;
    }
}
