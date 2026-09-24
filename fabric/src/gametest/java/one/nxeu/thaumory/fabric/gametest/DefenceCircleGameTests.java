package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.StartResult;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.TriggerResult;

/** The defence circles: lure, binding, searing and withering (requirements §17.4). */
public class DefenceCircleGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private static void start(GameTestHelper helper, CircleCoreBlockEntity core) {
        helper.assertValueEqual(core.start(Optional.empty()), StartResult.STARTED, "start");
    }

    @GameTest(maxTicks = 100)
    public void lureDrawsHostileMobsTowardsTheCore(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.BESTIA, ThaumoryAspects.VINCULUM);
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(7, 2, 7));
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 7));
        Vec3 centre = Vec3.atBottomCenterOf(helper.absolutePos(CORE));
        double huskStart = husk.position().distanceTo(centre);
        double cowStart = cow.position().distanceTo(centre);
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.position().distanceTo(centre) < huskStart - 1, "husk was not drawn in");
            helper.assertTrue(Math.abs(cow.position().distanceTo(centre) - cowStart) < 0.5, "an empty slot 3 drew a cow");
        });
    }

    @GameTest(maxTicks = 100)
    public void bindingSlowsTheChosenMobsHeavily(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.UMBRA, ThaumoryAspects.VINCULUM, ThaumoryAspects.BESTIA);
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(6, 2, 6));
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(2, 2, 6));
        start(helper, core);
        helper.succeedWhen(() -> {
            MobEffectInstance slowness = cow.getEffect(MobEffects.SLOWNESS);
            helper.assertTrue(slowness != null, "cow is not slowed");
            helper.assertValueEqual(slowness.getAmplifier(), 3, "slowness amplifier");
            helper.assertFalse(husk.hasEffect(MobEffects.SLOWNESS), "Bestia reached a husk");
        });
    }

    @GameTest(maxTicks = 40)
    public void searingBurnsHostileMobsAndSparesOthers(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.BELLUM, ThaumoryAspects.IGNIS);
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(6, 2, 6));
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 6));
        helper.assertValueEqual(core.trigger(Optional.empty()), TriggerResult.TRIGGERED, "trigger");
        helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "husk was not hurt");
        helper.assertTrue(husk.isOnFire(), "husk is not burning");
        helper.assertValueEqual(cow.getHealth(), cow.getMaxHealth(), "cow health");
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void searingDoesNotGoOffWithNoHostileMobInRange(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.BELLUM, ThaumoryAspects.IGNIS);
        helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 6));
        int essentia = core.essentia().amount(ThaumoryAspects.BELLUM);
        helper.assertValueEqual(core.trigger(Optional.empty()), TriggerResult.NO_TARGET, "trigger");
        helper.assertValueEqual(core.essentia().amount(ThaumoryAspects.BELLUM), essentia, "Bellum left");
        helper.succeed();
    }

    @GameTest(maxTicks = 200)
    public void witheringWearsDownHostileMobsEvenUndead(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.MORS, ThaumoryAspects.VENENUM);
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(6, 2, 6));
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertTrue(husk.hasEffect(MobEffects.WITHER), "husk is not withering");
            helper.assertTrue(husk.hasEffect(MobEffects.WEAKNESS), "husk is not weakened");
            helper.assertTrue(husk.getHealth() <= husk.getMaxHealth() - 2, "husk has not lost health");
        });
    }
}
