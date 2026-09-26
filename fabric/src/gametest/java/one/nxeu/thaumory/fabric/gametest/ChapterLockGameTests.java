package one.nxeu.thaumory.fabric.gametest;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.CrafterBlock;
import one.nxeu.thaumory.research.ResearchProgress;

/**
 * Crafting recipes a chapter unlocks (requirements §6.2). The test mod's chapter {@code
 * thaumory_gametest:locked_button} locks the dark oak button and can never be completed.
 */
public class ChapterLockGameTests {
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("dark_oak_button");

    @GameTest
    public void aCrafterMakesNoLockedRecipe(GameTestHelper helper) {
        CraftingInput planks = CraftingInput.of(1, 1, List.of(new ItemStack(Items.DARK_OAK_PLANKS)));
        helper.assertTrue(CrafterBlock.getPotentialResults(helper.getLevel(), planks).isEmpty(), "the crafter found the locked button");
        CraftingInput oak = CraftingInput.of(1, 1, List.of(new ItemStack(Items.OAK_PLANKS)));
        helper.assertTrue(CrafterBlock.getPotentialResults(helper.getLevel(), oak).isPresent(), "the crafter lost the open oak button");
        helper.succeed();
    }

    @GameTest
    public void aPlayerWithoutTheChapterCannotCraftIt(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.assertFalse(ResearchProgress.canUse(Optional.of(player), BUTTON), "the button is open");
        helper.assertTrue(ResearchProgress.canUse(Optional.of(player), Identifier.withDefaultNamespace("oak_button")), "the oak button is locked");
        helper.succeed();
    }
}
