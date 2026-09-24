package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.StartResult;

/** The farming circles: harvest, breeding and moisture (requirements §17.4). */
public class FarmCircleGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private static void start(GameTestHelper helper, CircleCoreBlockEntity core) {
        helper.assertValueEqual(core.start(Optional.empty()), StartResult.STARTED, "start");
    }

    @GameTest(maxTicks = 300)
    public void harvestReapsRipeCropsAndPlantsThemAgain(GameTestHelper helper) {
        Circles.floor(helper);
        helper.setBlock(7, 1, 1, Blocks.FARMLAND);
        helper.setBlock(7, 2, 1, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE));
        helper.setBlock(1, 1, 7, Blocks.FARMLAND);
        helper.setBlock(1, 2, 7, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
        // A pumpkin grown on its stem, and one set down by hand.
        helper.setBlock(1, 1, 1, Blocks.FARMLAND);
        helper.setBlock(1, 2, 1, Blocks.ATTACHED_PUMPKIN_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.EAST));
        helper.setBlock(2, 2, 1, Blocks.PUMPKIN);
        helper.setBlock(7, 2, 7, Blocks.PUMPKIN);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.HERBA, ThaumoryAspects.MORS);
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 1));
            helper.assertItemEntityPresent(Items.PUMPKIN);
            helper.assertItemEntityPresent(Items.WHEAT);
            helper.assertBlockProperty(new BlockPos(7, 2, 1), CropBlock.AGE, 0);
            helper.assertBlockProperty(new BlockPos(1, 2, 7), CropBlock.AGE, 3);
            helper.assertBlockPresent(Blocks.PUMPKIN, new BlockPos(7, 2, 7));
        });
    }

    @GameTest(maxTicks = 400)
    public void breedingPairsGrownAnimals(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.BESTIA, ThaumoryAspects.VITA);
        helper.spawn(EntityTypes.COW, new BlockPos(1, 2, 1));
        helper.spawn(EntityTypes.COW, new BlockPos(2, 2, 1));
        start(helper, core);
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 4, 8);
        helper.succeedWhen(() -> helper.assertTrue(
                !helper.getLevel().getEntitiesOfClass(Cow.class, area, Cow::isBaby).isEmpty(), "no calf was born"));
    }

    @GameTest(maxTicks = 300)
    public void moistureWetsFarmlandAndPutsOutFire(GameTestHelper helper) {
        Circles.floor(helper);
        helper.setBlock(7, 1, 1, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 0));
        helper.setBlock(7, 2, 1, Blocks.WHEAT);
        helper.setBlock(1, 2, 7, Blocks.FIRE);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(6, 2, 6));
        pig.igniteForSeconds(30);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.AQUA, ThaumoryAspects.HERBA);
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(new BlockPos(7, 1, 1), FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(1, 2, 7));
            helper.assertFalse(pig.isOnFire(), "pig is still burning");
        });
    }
}
