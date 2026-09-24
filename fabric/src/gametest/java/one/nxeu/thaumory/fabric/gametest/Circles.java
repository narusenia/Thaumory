package one.nxeu.thaumory.fabric.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;

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
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(core.offset(dx, -1, dz), Blocks.STONE);
                if (dx != 0 || dz != 0) {
                    helper.setBlock(core.offset(dx, 0, dz), ThaumoryBlocks.CHALK_LINE.get());
                }
            }
        }
        helper.setBlock(core, ThaumoryBlocks.CIRCLE_CORE.get());
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
