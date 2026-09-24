package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.StartResult;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.TriggerResult;

/** The eight MVP circle effects, one test per slot 3 behaviour (requirements §4.5). */
public class CircleEffectGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private static void start(GameTestHelper helper, CircleCoreBlockEntity core) {
        helper.assertValueEqual(core.start(Optional.empty()), StartResult.STARTED, "start");
    }

    private static Vec3 coreCentre(GameTestHelper helper) {
        return Vec3.atBottomCenterOf(helper.absolutePos(CORE));
    }

    @GameTest(maxTicks = 100)
    public void lightFillsDarkGroundAndClearsOnStop(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.LUX, ThaumoryAspects.IGNIS);
        start(helper, core);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(lights(helper) > 0, "no light placed"))
                .thenExecute(core::stop)
                .thenExecute(() -> helper.assertValueEqual(lights(helper), 0, "lights left after stopping"))
                .thenSucceed();
    }

    /** Light blocks anywhere in the circle's range, which reaches past the test area. */
    private static int lights(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(CORE);
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(core.offset(-4, -4, -4), core.offset(4, 4, 4))) {
            if (helper.getLevel().getBlockState(pos).is(Blocks.LIGHT)) {
                count++;
            }
        }
        return count;
    }

    @GameTest(maxTicks = 60)
    public void lightWithUmbraDarkensTheLiving(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.LUX, ThaumoryAspects.IGNIS, ThaumoryAspects.UMBRA);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, CORE.offset(2, 0, 0));
        start(helper, core);
        helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.DARKNESS), "pig is not in darkness"));
    }

    @GameTest(maxTicks = 100)
    public void wardWithBestiaPushesAnimalsOut(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VINCULUM, ThaumoryAspects.ORDO, ThaumoryAspects.BESTIA);
        Pig pig = helper.spawn(EntityTypes.PIG, CORE.offset(1, 0, 0));
        double before = pig.position().distanceTo(coreCentre(helper));
        start(helper, core);
        helper.succeedWhen(() -> helper.assertTrue(pig.position().distanceTo(coreCentre(helper)) > before + 1.5,
                "pig was not pushed away"));
    }

    @GameTest(maxTicks = 100)
    public void attractionDrawsItemsIn(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.TEMPESTAS, ThaumoryAspects.VINCULUM);
        ItemEntity item = helper.spawnItem(Items.STONE, new BlockPos(7, 2, 4));
        start(helper, core);
        helper.succeedWhen(() -> helper.assertTrue(item.position().distanceTo(coreCentre(helper)) < 1.5,
                "item was not drawn to the Core"));
    }

    @GameTest(maxTicks = 600, skyAccess = true)
    public void growthWithHerbaGrowsCrops(GameTestHelper helper) {
        helper.setTime(6000);
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                if (Math.abs(x - CORE.getX()) > 1 || Math.abs(z - CORE.getZ()) > 1) {
                    helper.setBlock(x, 1, z, Blocks.FARMLAND);
                    helper.setBlock(x, 2, z, Blocks.WHEAT);
                }
            }
        }
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.HERBA, ThaumoryAspects.VITA, ThaumoryAspects.HERBA);
        start(helper, core);
        helper.succeedWhen(() -> {
            boolean grown = false;
            for (int x = 0; x < 8 && !grown; x++) {
                for (int z = 0; z < 8 && !grown; z++) {
                    var state = helper.getBlockState(new BlockPos(x, 2, z));
                    grown = state.is(Blocks.WHEAT) && state.getValue(CropBlock.AGE) > 0;
                }
            }
            helper.assertTrue(grown, "no wheat grew");
        });
    }

    @GameTest(maxTicks = 60)
    public void growthWithBestiaRaisesYoungAnimals(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.HERBA, ThaumoryAspects.VITA, ThaumoryAspects.BESTIA);
        Cow calf = helper.spawnWithNoFreeWill(EntityTypes.COW, CORE.offset(2, 0, 0));
        calf.setAge(-24000);
        start(helper, core);
        helper.succeedWhen(() -> helper.assertTrue(calf.getAge() >= -24000 + 20, "calf did not age"));
    }

    @GameTest(maxTicks = 60)
    public void healingHealsTheLiving(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, CORE.offset(2, 0, 0));
        pig.setHealth(1);
        start(helper, core);
        helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() > 1, "pig was not healed"));
    }

    /** Far from other tests: purification takes Flux from every chunk its range touches. */
    @GameTest(maxTicks = 100, padding = 24)
    public void purificationTakesFluxAndRestoresPollution(GameTestHelper helper) {
        BlockPos polluted = CORE.offset(2, -1, 0);
        helper.setBlock(polluted, ThaumoryBlocks.POLLUTED_SOIL.get());
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ORDO, ThaumoryAspects.LUX);
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(CORE));
        Thaumory.flux().set(helper.getLevel(), chunk, 20);
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.DIRT, polluted);
            helper.assertTrue(ThaumoryApi.flux().get(helper.getLevel(), chunk) < 20, "Flux was not taken");
        });
    }

    @GameTest(maxTicks = 40)
    public void teleportCarriesToTheMatchingCircle(GameTestHelper helper) {
        BlockPos from = new BlockPos(1, 2, 1);
        BlockPos to = new BlockPos(6, 2, 6);
        CircleCoreBlockEntity here = Circles.build(helper, from, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER, ThaumoryAspects.TERRA);
        Circles.build(helper, to, ThaumoryAspects.AER, ThaumoryAspects.ARCANUM, ThaumoryAspects.TERRA);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, from);
        helper.assertValueEqual(here.trigger(Optional.of(pig)), TriggerResult.TRIGGERED, "trigger");
        helper.succeedWhen(() -> helper.assertTrue(pig.blockPosition().equals(helper.absolutePos(to)), "pig did not arrive"));
    }

    @GameTest(maxTicks = 40)
    public void weatherFollowsSlotThree(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        server.setWeatherParameters(6000, 0, false, false);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.TEMPESTAS, ThaumoryAspects.ARCANUM, ThaumoryAspects.AQUA);
        helper.assertValueEqual(core.trigger(Optional.empty()), TriggerResult.TRIGGERED, "trigger with Aqua");
        helper.assertTrue(server.getWeatherData().isRaining() && !server.getWeatherData().isThundering(), "Aqua did not bring rain");

        swapParameter(core, ThaumoryAspects.TEMPESTAS);
        helper.assertValueEqual(core.trigger(Optional.empty()), TriggerResult.TRIGGERED, "trigger with Tempestas");
        helper.assertTrue(server.getWeatherData().isThundering(), "Tempestas did not bring a thunderstorm");

        swapParameter(core, ThaumoryAspects.IGNIS);
        helper.assertValueEqual(core.trigger(Optional.empty()), TriggerResult.TRIGGERED, "trigger with Ignis");
        helper.assertFalse(server.getWeatherData().isRaining(), "Ignis did not clear the sky");
        helper.succeed();
    }

    /** A triggered circle pays from slot 3 too, so the new rune comes with its Essentia. */
    private static void swapParameter(CircleCoreBlockEntity core, Aspect parameter) {
        core.removeLast();
        core.insert(parameter.id());
        core.setEssentia(core.essentia().plus(AspectList.of(parameter, CircleCoreBlockEntity.capacity())));
    }

    @GameTest(maxTicks = 100)
    public void attractionWithAnAspectDrawsOnlyItemsHoldingIt(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.TEMPESTAS, ThaumoryAspects.VINCULUM, ThaumoryAspects.METALLUM);
        ItemEntity iron = helper.spawnItem(Items.IRON_INGOT, new BlockPos(7, 2, 3));
        ItemEntity stick = helper.spawnItem(Items.STICK, new BlockPos(7, 2, 5));
        double stickBefore = stick.position().distanceTo(coreCentre(helper));
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertTrue(iron.position().distanceTo(coreCentre(helper)) < 1.5, "iron was not drawn to the Core");
            helper.assertTrue(stick.position().distanceTo(coreCentre(helper)) > stickBefore - 0.5, "the stick was drawn too");
        });
    }

    @GameTest(maxTicks = 100)
    public void wardWithMorsPushesTheUndeadOut(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VINCULUM, ThaumoryAspects.ORDO, ThaumoryAspects.MORS);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, CORE.offset(1, 0, 0));
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, CORE.offset(0, 0, 2));
        double before = zombie.position().distanceTo(coreCentre(helper));
        Vec3 pigBefore = pig.position();
        start(helper, core);
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.position().distanceTo(coreCentre(helper)) > before + 1.5, "zombie was not pushed away");
            helper.assertTrue(pig.position().distanceTo(pigBefore) < 0.5, "the pig was pushed too");
        });
    }

    @GameTest(maxTicks = 20)
    public void aRunningCircleKeepsRunningWhenItsCoreLoadsAgain(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        start(helper, core);
        // What a chunk load does: a fresh Core read back from what was saved, ticked for the first time.
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CORE);
        CompoundTag saved = core.saveWithFullMetadata(level.registryAccess());
        CircleCoreBlockEntity loaded = (CircleCoreBlockEntity) BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
        loaded.setLevel(level);
        CircleCoreBlockEntity.serverTick(level, pos, level.getBlockState(pos), loaded);
        helper.assertTrue(loaded.isRunning(), "the circle stopped after loading");
        helper.succeed();
    }
}
