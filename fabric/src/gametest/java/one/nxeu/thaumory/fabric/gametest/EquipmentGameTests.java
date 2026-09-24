package one.nxeu.thaumory.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.item.ThaumoryItems;

/** The arcane metals' gear: Arcane Iron mines like iron, Aether Silver like diamond (requirements §10.2). */
public class EquipmentGameTests {
    @GameTest
    public void arcaneIronMinesLikeIron(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(ThaumoryItems.ARCANE_IRON.pickaxe().get());
        helper.assertTrue(pickaxe.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Arcane Iron cannot mine diamond ore");
        helper.assertFalse(pickaxe.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Arcane Iron mines obsidian");
        helper.assertValueEqual(pickaxe.getMaxDamage(), 350, "Arcane Iron durability");
        helper.succeed();
    }

    @GameTest
    public void aetherSilverMinesLikeDiamond(GameTestHelper helper) {
        ItemStack pickaxe = new ItemStack(ThaumoryItems.AETHER_SILVER.pickaxe().get());
        helper.assertTrue(pickaxe.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Aether Silver cannot mine obsidian");
        helper.assertValueEqual(pickaxe.getMaxDamage(), 1200, "Aether Silver durability");
        helper.succeed();
    }
}
