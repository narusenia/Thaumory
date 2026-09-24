package one.nxeu.thaumory.fabric.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import one.nxeu.thaumory.item.ArcaneLoupeItem;
import one.nxeu.thaumory.item.ThaumoryItems;

/** The monocle of revealing (requirements §17.6). */
public class MonocleGameTests {
    @GameTest(maxTicks = 20)
    public void wearingTheMonocleShowsWhatTheLoupeShows(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.assertFalse(ArcaneLoupeItem.sees(player), "sees with nothing");
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ThaumoryItems.MONOCLE.get()));
        helper.assertFalse(ArcaneLoupeItem.sees(player), "a monocle in hand is not worn");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ThaumoryItems.MONOCLE.get()));
        helper.assertTrue(ArcaneLoupeItem.sees(player), "does not see with the monocle on");
        helper.succeed();
    }
}
