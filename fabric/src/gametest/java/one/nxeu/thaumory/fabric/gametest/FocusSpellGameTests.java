package one.nxeu.thaumory.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.entity.ThaumoryEntities;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.wand.WandCasting;

/** What each focus casts (requirements §17.7). The caster stands at x 1.5, z 2.5 on the floor and looks east. */
public class FocusSpellGameTests {
    private static final AspectList FULL = AspectList.builder().add(ThaumoryAspects.IGNIS, 16).add(ThaumoryAspects.AQUA, 16)
            .add(ThaumoryAspects.TEMPESTAS, 16).add(ThaumoryAspects.TERRA, 16).add(ThaumoryAspects.BELLUM, 16).add(ThaumoryAspects.ARCANUM, 16)
            .add(ThaumoryAspects.AER, 16).add(ThaumoryAspects.ORDO, 16).add(ThaumoryAspects.CHAOS, 16).build();
    /** The block level with the caster's eyes, three blocks ahead. */
    private static final BlockPos AHEAD = new BlockPos(4, 3, 2);

    /** In the level so that teleporting and resting the wand reach a client; taken out when the test ends. */
    private static ServerPlayer caster(GameTestHelper helper, String focus, float pitch) {
        Circles.floor(helper);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(1.5, 2, 2.5)), -90, pitch);
        player.setYHeadRot(-90);
        ItemStack wand = new ItemStack(ThaumoryItems.WAND.get());
        wand.set(ThaumoryComponents.WAND_FOCUS.get(), Thaumory.id(focus));
        wand.set(ThaumoryComponents.STORED_ESSENTIA.get(), FULL);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        return player;
    }

    private static WandCasting.Result cast(GameTestHelper helper, ServerPlayer player) {
        return WandCasting.cast(helper.getLevel(), player, player.getMainHandItem());
    }

    private static int left(ServerPlayer player, Aspect aspect) {
        return player.getMainHandItem().getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty()).amount(aspect);
    }

    private static void leave(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static <T extends Mob> T still(GameTestHelper helper, EntityType<T> type, double x, double z) {
        T mob = helper.spawn(type, new Vec3(x, 2, z));
        mob.setNoAi(true);
        return mob;
    }

    /** A wall three blocks high across the caster's gaze at {@code x}. */
    private static void wall(GameTestHelper helper, int x, Block block) {
        for (int y = 2; y <= 4; y++) {
            helper.setBlock(x, y, 2, block);
        }
    }

    @GameTest(maxTicks = 60)
    public void theFireFocusBurnsWhatItHits(GameTestHelper helper) {
        // Aimed down at the pig's middle: 4 blocks on and about 1.2 below the eyes.
        ServerPlayer player = caster(helper, "fire_focus", 16);
        Mob pig = still(helper, EntityTypes.PIG, 5.5, 2.5);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.CAST, "cast");
        helper.assertValueEqual(left(player, ThaumoryAspects.IGNIS), 14, "Ignis left");
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.isOnFire(), "the pig is not burning");
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the pig is not hurt");
            helper.assertEntityNotPresent(ThaumoryEntities.FOCUS_FIREBALL.get());
            leave(helper, player);
        });
    }

    @GameTest
    public void theFrostFocusSlowsAndFreezesWater(GameTestHelper helper) {
        ServerPlayer player = caster(helper, "frost_focus", 0);
        Mob pig = still(helper, EntityTypes.PIG, 4.5, 2.5);
        helper.setBlock(4, 1, 3, Blocks.WATER);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.CAST, "cast");
        helper.assertTrue(pig.hasEffect(MobEffects.SLOWNESS), "the pig is not slowed");
        helper.assertBlockPresent(Blocks.FROSTED_ICE, new BlockPos(4, 1, 3));
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void theFrostFocusNeedsSomethingInItsCone(GameTestHelper helper) {
        ServerPlayer player = caster(helper, "frost_focus", 0);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.NOTHING, "cast");
        helper.assertValueEqual(left(player, ThaumoryAspects.AQUA), 16, "Aqua left");
        leave(helper, player);
        helper.succeed();
    }

    /** The bolt strikes the husk looked at, jumps to the husk beside it, and leaves the pig alone. */
    @GameTest
    public void theLightningFocusChainsToHostileMobs(GameTestHelper helper) {
        ServerPlayer player = caster(helper, "lightning_focus", 12);
        Mob first = still(helper, EntityTypes.HUSK, 5.5, 2.5);
        Mob second = still(helper, EntityTypes.HUSK, 5.5, 5.5);
        Mob pig = still(helper, EntityTypes.PIG, 6.5, 0.5);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.CAST, "cast");
        helper.assertTrue(first.getHealth() < first.getMaxHealth(), "the first husk is not hurt");
        helper.assertTrue(second.getHealth() < second.getMaxHealth(), "the second husk is not hurt");
        helper.assertTrue(first.getMaxHealth() - first.getHealth() > second.getMaxHealth() - second.getHealth(), "the jump hurt as much");
        helper.assertValueEqual(pig.getHealth(), pig.getMaxHealth(), "the pig's health");
        helper.assertFalse(first.isOnFire(), "the husk is burning");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void theDiggingFocusBreaksWhatTheCoreCan(GameTestHelper helper) {
        ServerPlayer player = caster(helper, "digging_focus", 0);
        wall(helper, 4, Blocks.STONE);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.CAST, "cast at stone");
        helper.assertBlockNotPresent(Blocks.STONE, AHEAD);
        helper.assertItemEntityPresent(Items.COBBLESTONE);
        // A wooden core digs like a stone pickaxe: no obsidian, and never bedrock.
        wall(helper, 5, Blocks.OBSIDIAN);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.NOTHING, "cast at obsidian");
        helper.setBlock(AHEAD.east(), Blocks.BEDROCK);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.NOTHING, "cast at bedrock");
        helper.assertValueEqual(left(player, ThaumoryAspects.TERRA), 15, "Terra left");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void theLeapFocusStopsShortOfAWall(GameTestHelper helper) {
        ServerPlayer player = caster(helper, "leap_focus", 0);
        wall(helper, 5, Blocks.STONE);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.CAST, "cast");
        helper.assertValueInBetween(4.4, player.getX() - helper.absoluteVec(Vec3.ZERO).x, 4.6, "x after the leap");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void theLeapFocusNeedsRoom(GameTestHelper helper) {
        ServerPlayer player = caster(helper, "leap_focus", 0);
        wall(helper, 2, Blocks.STONE);
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.NOTHING, "cast");
        helper.assertValueEqual(left(player, ThaumoryAspects.AER), 16, "Aer left");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest
    public void theExchangeFocusSwapsInTheOffHandBlock(GameTestHelper helper) {
        ServerPlayer player = caster(helper, "exchange_focus", 0);
        wall(helper, 4, Blocks.STONE);
        // The player comes in creative, which would not use up the planks.
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS, 3));
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.CAST, "cast");
        helper.assertBlockPresent(Blocks.OAK_PLANKS, AHEAD);
        helper.assertValueEqual(player.getOffhandItem().getCount(), 2, "planks left");
        helper.assertValueEqual(player.getInventory().countItem(Items.COBBLESTONE), 1, "cobblestone taken");
        helper.assertValueEqual(cast(helper, player), WandCasting.Result.NOTHING, "cast at the same block");
        leave(helper, player);
        helper.succeed();
    }
}
