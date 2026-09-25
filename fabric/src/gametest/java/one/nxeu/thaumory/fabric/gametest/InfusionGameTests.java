package one.nxeu.thaumory.fabric.gametest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.InfuseResult;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.circle.InfusionSettings;
import one.nxeu.thaumory.infusion.Infusion;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ScrollItem;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * Infusing the item on a pedestal with a circle's effect (requirements §10.1). Failure is a roll,
 * so each test sets the chance to never or always and puts the settings back after.
 */
public class InfusionGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);
    /** One use of teleport: its cost of 4 from each rune's aspect. */
    private static final Map<Identifier, Integer> TELEPORT_COST = Map.of(ThaumoryAspects.ARCANUM.id(), 4, ThaumoryAspects.AER.id(), 4);
    private static final Map<Identifier, Integer> PURIFICATION_COST = Map.of(ThaumoryAspects.ORDO.id(), 4, ThaumoryAspects.LUX.id(), 4);

    private static void failureChance(double chance) {
        CircleSettings current = CircleCoreBlockEntity.settings();
        InfusionSettings infusion = current.infusion();
        CircleCoreBlockEntity.updateSettings(new CircleSettings(current.scanInterval(), current.instabilityThreshold(), current.instabilityFlux(),
                current.undefinedFlux(), current.childInstability(), current.ringRadius(), current.rankStrength(), current.essentiaCapacity(), current.patterns(),
                new InfusionSettings(chance, infusion.failurePerPoint(), infusion.fluxRatio(), infusion.maxLevel(), infusion.itemEssentia())));
    }

    private static void restore() {
        CircleCoreBlockEntity.updateSettings(CircleSettings.DEFAULT);
    }

    /** Builds a pedestal into the core and puts {@code item} on it. */
    private static void pedestal(GameTestHelper helper, CircleCoreBlockEntity core, ItemStack item) {
        helper.setBlock(CORE, helper.getBlockState(CORE).setValue(CircleCoreBlock.PEDESTAL, true));
        core.setPedestalItem(item);
    }

    private static List<Infusion> infusions(CircleCoreBlockEntity core) {
        return core.pedestalItem().getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY).list();
    }

    @GameTest
    public void infusesTheItemAndPaysForIt(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        pedestal(helper, core, new ItemStack(ThaumoryItems.ARCANE_IRON.chestplate().get()));
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core), List.of(new Infusion(Thaumory.id("healing"), 1, Optional.empty(), 2)), "infusions");
        helper.assertValueEqual(core.essentia(), AspectList.builder().add(ThaumoryAspects.VITA, 32).add(ThaumoryAspects.ORDO, 32).build(),
                "Essentia left");
        helper.succeed();
    }

    @GameTest
    public void anAmplifierRaisesTheLevelAndTheCost(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        helper.setBlock(CORE.east(), ThaumoryBlocks.AMPLIFYING_PATTERN.get());
        core.rescan();
        pedestal(helper, core, new ItemStack(ThaumoryItems.ARCANE_IRON.chestplate().get()));
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core), List.of(new Infusion(Thaumory.id("healing"), 2, Optional.empty(), 2)), "infusions");
        helper.assertValueEqual(core.essentia(), AspectList.builder().add(ThaumoryAspects.VITA, 16).add(ThaumoryAspects.ORDO, 16).build(),
                "Essentia left");
        helper.succeed();
    }

    @GameTest
    public void slotThreeGoesIntoTheItem(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER, ThaumoryAspects.TERRA);
        pedestal(helper, core, new ItemStack(ThaumoryItems.ARCANE_IRON.sword().get()));
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core),
                List.of(new Infusion(Thaumory.id("teleport"), 1, Optional.of(ThaumoryAspects.TERRA.id()), 3, TELEPORT_COST)), "infusions");
        helper.assertValueEqual(core.essentia().amount(ThaumoryAspects.TERRA), CircleCoreBlockEntity.capacity() - 4, "Terra left");
        helper.succeed();
    }

    @GameTest
    public void itemsWithoutCapacityAndMissingEssentiaAreRefused(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        pedestal(helper, core, new ItemStack(Items.IRON_CHESTPLATE));
        helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.NO_CAPACITY, "iron chestplate");
        core.setPedestalItem(new ItemStack(ThaumoryItems.ARCANE_IRON.chestplate().get()));
        core.setEssentia(AspectList.builder().add(ThaumoryAspects.VITA, 31).add(ThaumoryAspects.ORDO, 64).build());
        helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.NO_ESSENTIA, "short of Vita");
        helper.assertValueEqual(core.essentia().amount(ThaumoryAspects.VITA), 31, "Vita left");
        helper.assertValueEqual(infusions(core), List.of(), "infusions");
        helper.succeed();
    }

    /** Arcane Iron holds 3: healing (2) and teleport (3) do not fit together, and nothing is paid. */
    @GameTest
    public void anEffectOverTheCapacityIsRefused(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER, ThaumoryAspects.TERRA);
        ItemStack sword = new ItemStack(ThaumoryItems.ARCANE_IRON.sword().get());
        Infusion healing = new Infusion(Thaumory.id("healing"), 1, Optional.empty(), 2);
        sword.set(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY.with(healing));
        pedestal(helper, core, sword);
        AspectList before = core.essentia();
        failureChance(0);
        try {
            CircleCoreBlockEntity.InfuseOutcome outcome = core.infuse(Optional.empty());
            helper.assertValueEqual(outcome.result(), InfuseResult.NO_ROOM, "result");
            helper.assertValueEqual(outcome.used(), 2, "used");
            helper.assertValueEqual(outcome.capacity(), 3, "capacity");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core), List.of(healing), "infusions");
        helper.assertValueEqual(core.essentia(), before, "Essentia");
        helper.succeed();
    }

    /** Burning the same effect in again only needs room for the difference. */
    @GameTest
    public void reinfusingAnEffectReusesItsShare(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER, ThaumoryAspects.TERRA);
        ItemStack sword = new ItemStack(ThaumoryItems.ARCANE_IRON.sword().get());
        sword.set(ThaumoryComponents.INFUSIONS.get(),
                Infusions.EMPTY.with(new Infusion(Thaumory.id("teleport"), 1, Optional.of(ThaumoryAspects.AQUA.id()), 3, TELEPORT_COST)));
        pedestal(helper, core, sword);
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core),
                List.of(new Infusion(Thaumory.id("teleport"), 1, Optional.of(ThaumoryAspects.TERRA.id()), 3, TELEPORT_COST)), "infusions");
        helper.succeed();
    }

    /** An item holds one active effect: purification does not go in beside teleport, though there is room. */
    @GameTest
    public void aSecondActiveEffectIsRefused(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ORDO, ThaumoryAspects.LUX);
        ItemStack chestplate = new ItemStack(ThaumoryItems.AETHER_SILVER.chestplate().get());
        chestplate.set(ThaumoryComponents.INFUSIONS.get(),
                Infusions.EMPTY.with(new Infusion(Thaumory.id("teleport"), 1, Optional.of(ThaumoryAspects.TERRA.id()), 3, TELEPORT_COST)));
        pedestal(helper, core, chestplate);
        helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.ACTIVE_TAKEN, "result");
        helper.succeed();
    }

    /** A charging circle fills what the item on its pedestal stores, and is started by the wand rather than infused. */
    @GameTest(maxTicks = 100)
    public void aChargingCircleFillsThePedestalItem(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ARCANUM, ThaumoryAspects.VINCULUM, ThaumoryAspects.AER);
        ItemStack sword = new ItemStack(ThaumoryItems.ARCANE_IRON.sword().get());
        sword.set(ThaumoryComponents.INFUSIONS.get(),
                Infusions.EMPTY.with(new Infusion(Thaumory.id("teleport"), 1, Optional.of(ThaumoryAspects.TERRA.id()), 3, TELEPORT_COST)));
        pedestal(helper, core, sword);
        helper.assertFalse(core.infusesPedestalItem(), "a charging circle would infuse");
        helper.assertValueEqual(core.start(Optional.empty()), CircleCoreBlockEntity.StartResult.STARTED, "start");
        helper.succeedWhen(() -> {
            AspectList stored = core.pedestalItem().getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
            helper.assertTrue(stored.amount(ThaumoryAspects.ARCANUM) >= 8, "Arcanum stored: " + stored);
            helper.assertTrue(stored.amount(ThaumoryAspects.AER) >= 8, "Aer stored: " + stored);
            helper.assertValueEqual(stored.amount(ThaumoryAspects.VINCULUM), 0, "Vinculum stored");
        });
    }

    /** Using an active effect pays from the item, and nothing happens once it runs dry. Checks its chunk's Flux. */
    @GameTest(padding = 24)
    public void usingAnActiveEffectPaysFromTheItem(GameTestHelper helper) {
        Circles.floor(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
        ItemStack sword = new ItemStack(ThaumoryItems.ARCANE_IRON.sword().get());
        sword.set(ThaumoryComponents.INFUSIONS.get(),
                Infusions.EMPTY.with(new Infusion(Thaumory.id("purification"), 1, Optional.empty(), 2, PURIFICATION_COST)));
        sword.set(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.builder().add(ThaumoryAspects.ORDO, 6).add(ThaumoryAspects.LUX, 4).build());
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        ChunkPos chunk = ChunkPos.containing(player.blockPosition());
        Thaumory.flux().set(helper.getLevel(), chunk, 30);

        helper.assertValueEqual(InfusionRuntime.tryUse(player), InfusionRuntime.UseResult.USED, "first use");
        helper.assertValueInBetween(19.9, Thaumory.flux().get(helper.getLevel(), chunk), 20.0, "Flux");
        helper.assertValueEqual(player.getMainHandItem().get(ThaumoryComponents.STORED_ESSENTIA.get()),
                AspectList.of(ThaumoryAspects.ORDO, 2), "stored");
        helper.assertValueEqual(InfusionRuntime.tryUse(player), InfusionRuntime.UseResult.NO_ESSENTIA, "second use");
        helper.succeed();
    }

    /** One blank scroll off a stack goes on the pedestal, and infusing it makes a scroll that takes nothing more. */
    @GameTest
    public void aBlankScrollTurnsIntoAScroll(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        helper.setBlock(CORE, helper.getBlockState(CORE).setValue(CircleCoreBlock.PEDESTAL, true));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumoryItems.BLANK_SCROLL.get(), 5));
        helper.useBlock(CORE, player);
        helper.assertValueEqual(core.pedestalItem().getCount(), 1, "scrolls on the pedestal");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 4, "scrolls left in hand");
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "first");
            helper.assertValueEqual(core.pedestalItem().getItem(), ThaumoryItems.SCROLL.get(), "item");
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.NO_CAPACITY, "second");
        } finally {
            restore();
        }
        helper.succeed();
    }

    /** A healing scroll heals at once and is used up; a teleport scroll with nowhere to go is kept. */
    @GameTest
    public void aScrollWorksOnceAndOnlyWhenItCan(GameTestHelper helper) {
        Circles.floor(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
        player.setHealth(10);
        ItemStack healing = new ItemStack(ThaumoryItems.SCROLL.get(), 2);
        healing.set(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY.with(new Infusion(Thaumory.id("healing"), 1, Optional.empty(), 2)));
        player.setItemInHand(InteractionHand.MAIN_HAND, healing);
        helper.assertTrue(ScrollItem.cast(helper.getLevel(), player, player.getMainHandItem()), "healing did not cast");
        helper.assertValueEqual(player.getHealth(), 14.0f, "health");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "healing scrolls left");

        ItemStack teleport = new ItemStack(ThaumoryItems.SCROLL.get());
        teleport.set(ThaumoryComponents.INFUSIONS.get(),
                Infusions.EMPTY.with(new Infusion(Thaumory.id("teleport"), 1, Optional.of(ThaumoryAspects.VENENUM.id()), 3, TELEPORT_COST)));
        player.setItemInHand(InteractionHand.MAIN_HAND, teleport);
        helper.assertFalse(ScrollItem.cast(helper.getLevel(), player, player.getMainHandItem()), "teleport cast with nowhere to go");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 1, "teleport scroll kept");
        // Held, a scroll is not equipment: the key finds nothing to use.
        helper.assertValueEqual(InfusionRuntime.tryUse(player), InfusionRuntime.UseResult.NONE, "key with a scroll in hand");
        helper.succeed();
    }

    /** An amulet deep in the inventory is used by the key when nothing in hand or worn has an active effect. Checks its chunk's Flux. */
    @GameTest(padding = 24)
    public void theKeyFindsAnAmuletInTheInventory(GameTestHelper helper) {
        Circles.floor(helper);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
        ItemStack amulet = new ItemStack(ThaumoryItems.AMULET.get());
        amulet.set(ThaumoryComponents.INFUSIONS.get(),
                Infusions.EMPTY.with(new Infusion(Thaumory.id("purification"), 1, Optional.empty(), 2, PURIFICATION_COST)));
        amulet.set(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.builder().add(ThaumoryAspects.ORDO, 4).add(ThaumoryAspects.LUX, 4).build());
        player.getInventory().setItem(20, amulet);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumoryItems.ARCANE_IRON.sword().get()));
        ChunkPos chunk = ChunkPos.containing(player.blockPosition());
        Thaumory.flux().set(helper.getLevel(), chunk, 30);

        helper.assertValueEqual(InfusionRuntime.tryUse(player), InfusionRuntime.UseResult.USED, "use");
        helper.assertValueInBetween(19.9, Thaumory.flux().get(helper.getLevel(), chunk), 20.0, "Flux");
        helper.assertTrue(player.getInventory().getItem(20).get(ThaumoryComponents.STORED_ESSENTIA.get()) == null, "the amulet did not pay");
        helper.succeed();
    }

    /** Kept apart from other tests, since it checks the Flux of its whole chunk. */
    @GameTest(padding = 24)
    public void aFailureLosesTheEssentiaToFlux(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        pedestal(helper, core, new ItemStack(ThaumoryItems.ARCANE_IRON.chestplate().get()));
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(CORE));
        Thaumory.flux().set(helper.getLevel(), chunk, 0);
        failureChance(1);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.FAILED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core), List.of(), "infusions");
        helper.assertValueEqual(core.pedestalItem().getItem(), ThaumoryItems.ARCANE_IRON.chestplate().get(), "item");
        helper.assertValueEqual(core.essentia(), AspectList.builder().add(ThaumoryAspects.VITA, 32).add(ThaumoryAspects.ORDO, 32).build(),
                "Essentia left");
        helper.assertValueInBetween(31.9, Thaumory.flux().get(helper.getLevel(), chunk), 32.0, "Flux");
        helper.succeed();
    }

    @GameTest
    public void aPedestalIsBuiltInAndTakenOutWithItsItem(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumoryItems.PEDESTAL.get()));
        helper.useBlock(CORE, player);
        helper.assertBlockProperty(CORE, CircleCoreBlock.PEDESTAL, true);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "the pedestal was not used up");

        // A tool keeps its own use, a stackable item does not go on, a sword does.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumoryItems.ARCANE_LOUPE.get()));
        helper.useBlock(CORE, player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        helper.useBlock(CORE, player);
        helper.assertTrue(core.pedestalItem().isEmpty(), "a tool or a stackable item went on the pedestal");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.useBlock(CORE, player);
        helper.assertValueEqual(core.pedestalItem().getItem(), Items.IRON_SWORD, "item on the pedestal");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThaumoryItems.WAND.get()));
        helper.useBlock(CORE, player);
        helper.assertValueEqual(core.pedestalItem().getItem(), Items.IRON_SWORD, "item on the pedestal after a wand's click");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.useBlock(CORE, player);
        helper.assertTrue(core.pedestalItem().isEmpty(), "an empty hand did not take the item");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.IRON_SWORD)), "the sword did not come to the hand");
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.useBlock(CORE, player);

        core.removePedestal(player);
        helper.assertBlockProperty(CORE, CircleCoreBlock.PEDESTAL, false);
        helper.assertTrue(core.pedestalItem().isEmpty(), "the item stayed");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.IRON_SWORD)), "the sword did not come back");
        helper.assertTrue(player.getInventory().contains(new ItemStack(ThaumoryItems.PEDESTAL.get())), "the pedestal did not come back");
        helper.succeed();
    }
}
