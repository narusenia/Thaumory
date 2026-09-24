package one.nxeu.thaumory.fabric.gametest;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
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
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * Infusing the item on a pedestal with a circle's effect (requirements §10.1). Failure is a roll,
 * so each test sets the chance to never or always and puts the settings back after.
 */
public class InfusionGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);

    private static void failureChance(double chance) {
        CircleSettings current = CircleCoreBlockEntity.settings();
        InfusionSettings infusion = current.infusion();
        CircleCoreBlockEntity.updateSettings(new CircleSettings(current.scanInterval(), current.instabilityThreshold(), current.instabilityFlux(),
                current.undefinedFlux(), current.ringRadius(), current.essentiaCapacity(), current.patterns(),
                new InfusionSettings(chance, infusion.failurePerPoint(), infusion.fluxRatio(), infusion.maxLevel())));
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
        pedestal(helper, core, new ItemStack(Items.IRON_CHESTPLATE));
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core), List.of(new Infusion(Thaumory.id("healing"), 1, Optional.empty())), "infusions");
        helper.assertValueEqual(core.essentia(), AspectList.builder().add(ThaumoryAspects.VITA, 32).add(ThaumoryAspects.ORDO, 32).build(),
                "Essentia left");
        helper.succeed();
    }

    @GameTest
    public void anAmplifierRaisesTheLevelAndTheCost(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        helper.setBlock(CORE.east(), ThaumoryBlocks.AMPLIFYING_PATTERN.get());
        core.rescan();
        pedestal(helper, core, new ItemStack(Items.IRON_CHESTPLATE));
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core), List.of(new Infusion(Thaumory.id("healing"), 2, Optional.empty())), "infusions");
        helper.assertValueEqual(core.essentia(), AspectList.builder().add(ThaumoryAspects.VITA, 16).add(ThaumoryAspects.ORDO, 16).build(),
                "Essentia left");
        helper.succeed();
    }

    @GameTest
    public void slotThreeGoesIntoTheItem(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.ARCANUM, ThaumoryAspects.AER, ThaumoryAspects.TERRA);
        pedestal(helper, core, new ItemStack(Items.IRON_SWORD));
        failureChance(0);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.INFUSED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core),
                List.of(new Infusion(Thaumory.id("teleport"), 1, Optional.of(ThaumoryAspects.TERRA.id()))), "infusions");
        helper.assertValueEqual(core.essentia().amount(ThaumoryAspects.TERRA), CircleCoreBlockEntity.capacity() - 4, "Terra left");
        helper.succeed();
    }

    @GameTest
    public void stackableItemsAndMissingEssentiaAreRefused(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        pedestal(helper, core, new ItemStack(Items.STONE));
        helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.STACKABLE, "stone");
        core.setPedestalItem(new ItemStack(Items.IRON_CHESTPLATE));
        core.setEssentia(AspectList.builder().add(ThaumoryAspects.VITA, 31).add(ThaumoryAspects.ORDO, 64).build());
        helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.NO_ESSENTIA, "short of Vita");
        helper.assertValueEqual(core.essentia().amount(ThaumoryAspects.VITA), 31, "Vita left");
        helper.assertValueEqual(infusions(core), List.of(), "infusions");
        helper.succeed();
    }

    /** Kept apart from other tests, since it checks the Flux of its whole chunk. */
    @GameTest(padding = 24)
    public void aFailureLosesTheEssentiaToFlux(GameTestHelper helper) {
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VITA, ThaumoryAspects.ORDO);
        pedestal(helper, core, new ItemStack(Items.IRON_CHESTPLATE));
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(CORE));
        Thaumory.flux().set(helper.getLevel(), chunk, 0);
        failureChance(1);
        try {
            helper.assertValueEqual(core.infuse(Optional.empty()).result(), InfuseResult.FAILED, "result");
        } finally {
            restore();
        }
        helper.assertValueEqual(infusions(core), List.of(), "infusions");
        helper.assertValueEqual(core.pedestalItem().getItem(), Items.IRON_CHESTPLATE, "item");
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
        // The wand's click infused the sword, so look for any sword.
        helper.assertTrue(player.getInventory().hasAnyMatching(stack -> stack.is(Items.IRON_SWORD)), "the sword did not come to the hand");
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
