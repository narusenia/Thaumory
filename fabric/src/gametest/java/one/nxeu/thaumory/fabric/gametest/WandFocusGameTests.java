package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.wand.WandCasting;

/** Casting from a wand's focus, and filling the wand for it (requirements §17.7). */
public class WandFocusGameTests {
    /** Where the light goes: in front of the wall, level with the caster's eyes. */
    private static final BlockPos SPOT = new BlockPos(3, 3, 2);

    private static ItemStack lightWand(int lux) {
        ItemStack wand = new ItemStack(ThaumoryItems.WAND.get());
        wand.set(ThaumoryComponents.WAND_FOCUS.get(), Thaumory.id("light_focus"));
        if (lux > 0) {
            wand.set(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.of(ThaumoryAspects.LUX, lux));
        }
        return wand;
    }

    private static AspectList stored(ItemStack wand) {
        return wand.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
    }

    /** A caster at x 1.5 looking east at a stone wall at x 4. */
    private static ServerPlayer casterFacingWall(GameTestHelper helper) {
        Circles.floor(helper);
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(4, y, 2, Blocks.STONE);
        }
        // In the level, so resting the wand can reach the client; taken out again when the test ends.
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(1.5, 2, 2.5)), -90, 0);
        // Where a player aims is where their head looks.
        player.setYHeadRot(-90);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    @GameTest
    public void theLightFocusSetsALightWhereTheCasterLooks(GameTestHelper helper) {
        ServerPlayer player = casterFacingWall(helper);
        ItemStack wand = lightWand(3);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        helper.assertValueEqual(WandCasting.cast(helper.getLevel(), player, wand), WandCasting.Result.CAST, "cast");
        helper.assertBlockPresent(Blocks.LIGHT, SPOT);
        helper.assertValueEqual(stored(wand), AspectList.of(ThaumoryAspects.LUX, 2), "left in the wand");
        helper.assertTrue(player.getCooldowns().isOnCooldown(wand), "the wand does not rest");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void aWandWithoutEssentiaCastsNothing(GameTestHelper helper) {
        ServerPlayer player = casterFacingWall(helper);
        ItemStack wand = lightWand(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        helper.assertValueEqual(WandCasting.cast(helper.getLevel(), player, wand), WandCasting.Result.NO_ESSENTIA, "cast");
        helper.assertBlockNotPresent(Blocks.LIGHT, SPOT);
        helper.assertFalse(player.getCooldowns().isOnCooldown(wand), "the wand rests");
        leave(helper, player);
        helper.succeed();
    }

    /** Water fills the spot in front of the wall, so there is nowhere for the light. */
    @GameTest
    public void castingWithNowhereForTheLightPaysNothing(GameTestHelper helper) {
        ServerPlayer player = casterFacingWall(helper);
        helper.setBlock(SPOT, Blocks.WATER);
        ItemStack wand = lightWand(3);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        helper.assertValueEqual(WandCasting.cast(helper.getLevel(), player, wand), WandCasting.Result.NOTHING, "cast");
        helper.assertValueEqual(stored(wand), AspectList.of(ThaumoryAspects.LUX, 3), "left in the wand");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void sneakingTakesALightAwayForFree(GameTestHelper helper) {
        ServerPlayer player = casterFacingWall(helper);
        helper.setBlock(SPOT.above(), Blocks.LIGHT);
        player.setShiftKeyDown(true);
        ItemStack wand = lightWand(3);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);

        helper.assertValueEqual(WandCasting.cast(helper.getLevel(), player, wand), WandCasting.Result.CAST, "cast");
        helper.assertBlockNotPresent(Blocks.LIGHT, SPOT.above());
        helper.assertValueEqual(stored(wand), AspectList.of(ThaumoryAspects.LUX, 3), "left in the wand");
        leave(helper, player);
        helper.succeed();
    }

    /** The wand takes only what its focus uses, as far as its gold caps hold, and keeps the rest of an earlier focus. */
    @GameTest
    public void aWandTakesWhatItsFocusUsesUpToItsCaps(GameTestHelper helper) {
        ItemStack wand = lightWand(0);
        wand.set(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.of(ThaumoryAspects.IGNIS, 5));
        AspectList taken = WandCasting.fill(wand, AspectList.builder().add(ThaumoryAspects.LUX, 20).add(ThaumoryAspects.AQUA, 20).build());

        helper.assertValueEqual(taken, AspectList.of(ThaumoryAspects.LUX, 16), "taken");
        helper.assertValueEqual(stored(wand), AspectList.builder().add(ThaumoryAspects.LUX, 16).add(ThaumoryAspects.IGNIS, 5).build(), "stored");
        helper.assertValueEqual(WandCasting.fill(new ItemStack(ThaumoryItems.WAND.get()), AspectList.of(ThaumoryAspects.LUX, 20)),
                AspectList.empty(), "taken without a focus");
        helper.succeed();
    }

    @GameTest
    public void aJarPoursIntoTheWandInTheOtherHand(GameTestHelper helper) {
        Circles.floor(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
        player.setShiftKeyDown(true);
        ItemStack jar = new ItemStack(ThaumoryItems.JAR.get());
        jar.set(ThaumoryComponents.JAR_CONTENTS.get(), new JarContents(AspectList.of(ThaumoryAspects.LUX, 20), Optional.empty()));
        player.setItemInHand(InteractionHand.MAIN_HAND, jar);
        player.setItemInHand(InteractionHand.OFF_HAND, lightWand(0));

        jar.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(stored(player.getOffhandItem()), AspectList.of(ThaumoryAspects.LUX, 16), "in the wand");
        helper.assertValueEqual(player.getMainHandItem().get(ThaumoryComponents.JAR_CONTENTS.get()).aspects(),
                AspectList.of(ThaumoryAspects.LUX, 4), "left in the jar");
        helper.succeed();
    }

    /** A charging circle with a Lux rune fills the wand a player in its range holds. */
    @GameTest(maxTicks = 100)
    public void aChargingCircleFillsAHeldWand(GameTestHelper helper) {
        BlockPos corePos = new BlockPos(4, 2, 4);
        CircleCoreBlockEntity core = Circles.build(helper, corePos, ThaumoryAspects.ARCANUM, ThaumoryAspects.VINCULUM, ThaumoryAspects.LUX);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(4.5, 2, 5.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND, lightWand(0));
        helper.assertValueEqual(core.start(Optional.empty()), CircleCoreBlockEntity.StartResult.STARTED, "start");
        helper.succeedWhen(() -> {
            AspectList stored = stored(player.getMainHandItem());
            helper.assertTrue(stored.amount(ThaumoryAspects.LUX) >= 8, "Lux in the wand: " + stored);
            leave(helper, player);
        });
    }
}
