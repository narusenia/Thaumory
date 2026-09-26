package one.nxeu.thaumory.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.infusion.Infusion;
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.pouch.EssentiaPouchItem;
import one.nxeu.thaumory.pouch.PouchRuntime;

/** An Essentia pouch topping up what keeps Essentia (requirements §17.6). */
public class PouchGameTests {
    private static final Map<Identifier, Integer> TELEPORT_COST = Map.of(ThaumoryAspects.ARCANUM.id(), 4, ThaumoryAspects.AER.id(), 4);

    private static ItemStack jar(AspectList aspects, Optional<Aspect> label) {
        ItemStack jar = new ItemStack(ThaumoryItems.JAR.get());
        jar.set(ThaumoryComponents.JAR_CONTENTS.get(), new JarContents(aspects, label));
        return jar;
    }

    private static ItemStack pouch(ItemStack... jars) {
        ItemStack pouch = new ItemStack(ThaumoryItems.ESSENTIA_POUCH.get());
        List<ItemStack> slots = new ArrayList<>(List.of(jars));
        while (slots.size() < EssentiaPouchItem.SLOTS) {
            slots.add(ItemStack.EMPTY);
        }
        EssentiaPouchItem.setJars(pouch, slots);
        return pouch;
    }

    private static AspectList jarAspects(ItemStack pouch, int slot) {
        return EssentiaPouchItem.jars(pouch).get(slot).getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY).aspects();
    }

    private static AspectList stored(ItemStack item) {
        return item.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
    }

    /** The wand takes only the Lux its focus uses, 4 a round, and the Ignis stays in the jar. */
    @GameTest
    public void aPouchTopsUpTheWandInHandWithWhatItsFocusUses(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack wand = new ItemStack(ThaumoryItems.WAND.get());
        wand.set(ThaumoryComponents.WAND_FOCUS.get(), Thaumory.id("light_focus"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().setItem(20, pouch(jar(AspectList.builder().add(ThaumoryAspects.LUX, 10).add(ThaumoryAspects.IGNIS, 10).build(),
                Optional.empty())));

        helper.assertTrue(PouchRuntime.refill(player), "nothing moved");
        helper.assertValueEqual(stored(player.getMainHandItem()), AspectList.of(ThaumoryAspects.LUX, 4), "in the wand");
        helper.assertValueEqual(jarAspects(player.getInventory().getItem(20), 0),
                AspectList.builder().add(ThaumoryAspects.LUX, 6).add(ThaumoryAspects.IGNIS, 10).build(), "left in the jar");
        helper.succeed();
    }

    /** Worn equipment draws from the first jar, then the next; the emptied jar stays, label and all. */
    @GameTest
    public void aPouchTopsUpEquipmentFromItsJarsInOrder(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack helmet = new ItemStack(ThaumoryItems.ARCANE_IRON.helmet().get());
        helmet.set(ThaumoryComponents.INFUSIONS.get(),
                Infusions.EMPTY.with(new Infusion(Thaumory.id("teleport"), 1, Optional.empty(), 3, TELEPORT_COST)));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.getInventory().setItem(20, pouch(jar(AspectList.of(ThaumoryAspects.ARCANUM, 1), Optional.of(ThaumoryAspects.ARCANUM)),
                jar(AspectList.builder().add(ThaumoryAspects.ARCANUM, 10).add(ThaumoryAspects.AER, 10).build(), Optional.empty())));

        helper.assertTrue(PouchRuntime.refill(player), "nothing moved");
        helper.assertValueEqual(stored(player.getItemBySlot(EquipmentSlot.HEAD)),
                AspectList.builder().add(ThaumoryAspects.ARCANUM, 4).add(ThaumoryAspects.AER, 4).build(), "in the helmet");
        ItemStack pouch = player.getInventory().getItem(20);
        ItemStack emptied = EssentiaPouchItem.jars(pouch).get(0);
        helper.assertTrue(emptied.is(ThaumoryItems.JAR.get()), "the emptied jar is gone");
        helper.assertValueEqual(emptied.get(ThaumoryComponents.JAR_CONTENTS.get()).label(), Optional.of(ThaumoryAspects.ARCANUM), "its label");
        helper.assertValueEqual(jarAspects(pouch, 1), AspectList.builder().add(ThaumoryAspects.ARCANUM, 7).add(ThaumoryAspects.AER, 6).build(),
                "left in the second jar");
        helper.succeed();
    }

    @GameTest
    public void withoutAPouchNothingMoves(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack wand = new ItemStack(ThaumoryItems.WAND.get());
        wand.set(ThaumoryComponents.WAND_FOCUS.get(), Thaumory.id("light_focus"));
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        helper.assertFalse(PouchRuntime.refill(player), "something moved");
        helper.succeed();
    }
}
