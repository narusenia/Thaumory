package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.StartResult;

/** The life circles: lightness, breath, night sight and safeguard (requirements §17.4). */
public class LifeCircleGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);
    /** Long enough for a started circle to have worked once. */
    private static final int WORKED = 30;

    private static void start(GameTestHelper helper, CircleCoreBlockEntity core) {
        helper.assertValueEqual(core.start(Optional.empty()), StartResult.STARTED, "start");
    }

    @GameTest(maxTicks = 100)
    public void lightnessSpeedsAnimalsAndTakesAwayTheirFallDamage(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.AER, ThaumoryAspects.TERRA, ThaumoryAspects.BESTIA);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(6, 2, 6));
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.hasEffect(MobEffects.SPEED), "pig is not sped up");
            float health = pig.getHealth();
            pig.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 6);
            helper.assertValueEqual(pig.getHealth(), health, "pig health after a fall");
        });
    }

    @GameTest(maxTicks = 100)
    public void breathLetsHostileMobsBreatheAndSwimWithChaos(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.AQUA, ThaumoryAspects.AER, ThaumoryAspects.CHAOS);
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(6, 2, 6));
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 6));
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.hasEffect(MobEffects.WATER_BREATHING), "husk cannot breathe underwater");
            helper.assertTrue(husk.hasEffect(MobEffects.DOLPHINS_GRACE), "husk does not swim faster");
            helper.assertFalse(cow.hasEffect(MobEffects.WATER_BREATHING), "Chaos reached a cow");
        });
    }

    @GameTest(maxTicks = 100)
    public void nightSightKeepsNightVisionAboveItsFlicker(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.LUX, ThaumoryAspects.UMBRA, ThaumoryAspects.BESTIA);
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(6, 2, 6));
        start(helper, core);
        helper.succeedWhen(() -> {
            MobEffectInstance vision = cow.getEffect(MobEffects.NIGHT_VISION);
            helper.assertTrue(vision != null, "cow has no night vision");
            helper.assertTrue(vision.getDuration() > 200, "night vision is short enough to flicker");
        });
    }

    @GameTest(maxTicks = 100)
    public void safeguardKeepsBlocksStandingInAnExplosion(GameTestHelper helper) {
        Circles.floor(helper);
        helper.setBlock(6, 2, 7, Blocks.STONE_BRICKS);
        helper.setBlock(7, 2, 6, Blocks.OAK_PLANKS);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ORDO, ThaumoryAspects.TERRA);
        start(helper, core);
        helper.runAfterDelay(WORKED, () -> {
            Vec3 centre = Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 2, 6)));
            helper.getLevel().explode(null, centre.x, centre.y, centre.z, 3, Level.ExplosionInteraction.TNT);
            helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(6, 2, 7));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(7, 2, 6));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 6));
            helper.assertBlockPresent(ThaumoryBlocks.CHALK_LINE.get(), new BlockPos(5, 2, 5));
            helper.assertTrue(core.isRunning(), "the circle broke");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void safeguardKeepsMobsFromTramplingFarmland(GameTestHelper helper) {
        Circles.floor(helper);
        helper.setBlock(6, 1, 6, Blocks.FARMLAND);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ORDO, ThaumoryAspects.TERRA);
        start(helper, core);
        helper.runAfterDelay(WORKED, () -> {
            Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(6, 6, 6));
            helper.succeedWhen(() -> {
                helper.assertTrue(pig.onGround(), "pig has not landed");
                helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(6, 1, 6));
            });
        });
    }

    /** The control for the test above: without a safeguard the same fall tramples the farmland. */
    @GameTest(maxTicks = 100)
    public void mobsTrampleFarmlandOutsideASafeguard(GameTestHelper helper) {
        Circles.floor(helper);
        helper.setBlock(6, 1, 6, Blocks.FARMLAND);
        helper.spawn(EntityTypes.PIG, new BlockPos(6, 6, 6));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.DIRT, new BlockPos(6, 1, 6)));
    }
}
