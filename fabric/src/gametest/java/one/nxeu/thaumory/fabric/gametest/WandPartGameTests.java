package one.nxeu.thaumory.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.wand.WandBuild;

/** Putting wands together from caps and cores and changing their parts (requirements §7.1). */
public class WandPartGameTests {
    private static CraftingInput grid(Object... cellsAndItems) {
        List<ItemStack> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            cells.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < cellsAndItems.length; i += 2) {
            Object item = cellsAndItems[i + 1];
            cells.set((Integer) cellsAndItems[i], item instanceof ItemStack stack ? stack : new ItemStack((ItemLike) item));
        }
        return CraftingInput.of(3, 3, cells);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> recipe(GameTestHelper helper, CraftingInput input) {
        return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
    }

    private static WandBuild build(ItemStack wand) {
        return wand.getOrDefault(ThaumoryComponents.WAND_BUILD.get(), WandBuild.DEFAULT);
    }

    @GameTest
    public void capsAndACoreMakeAWandOfThoseParts(GameTestHelper helper) {
        CraftingInput input = grid(2, ThaumoryItems.ARCANE_IRON_WAND_CAP.get(), 4, ThaumoryItems.CRYSTAL_WAND_CORE.get(), 6,
                ThaumoryItems.ARCANE_IRON_WAND_CAP.get());
        ItemStack wand = recipe(helper, input).orElseThrow().value().assemble(input);
        helper.assertTrue(wand.is(ThaumoryItems.WAND.get()), "no wand made");
        helper.assertValueEqual(build(wand), new WandBuild(Thaumory.id("arcane_iron_wand_cap"), Thaumory.id("crystal_wand_core")), "parts");
        helper.succeed();
    }

    @GameTest
    public void goldCapsOnAStickMakeTheOldWand(GameTestHelper helper) {
        CraftingInput input = grid(0, ThaumoryItems.GOLD_WAND_CAP.get(), 4, Items.STICK, 8, ThaumoryItems.GOLD_WAND_CAP.get());
        helper.assertValueEqual(build(recipe(helper, input).orElseThrow().value().assemble(input)), WandBuild.DEFAULT, "parts");
        helper.succeed();
    }

    @GameTest
    public void newCapsGoOnAndTheOldOnesComeBack(GameTestHelper helper) {
        ItemStack old = new ItemStack(ThaumoryItems.WAND.get());
        old.set(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.of(ThaumoryAspects.IGNIS, 5));
        CraftingInput input = grid(0, old, 3, ThaumoryItems.AETHER_SILVER_WAND_CAP.get(), 5, ThaumoryItems.AETHER_SILVER_WAND_CAP.get());
        CraftingRecipe recipe = recipe(helper, input).orElseThrow().value();
        ItemStack wand = recipe.assemble(input);
        helper.assertValueEqual(build(wand), WandBuild.DEFAULT.withCap(Thaumory.id("aether_silver_wand_cap")), "parts");
        helper.assertValueEqual(wand.get(ThaumoryComponents.STORED_ESSENTIA.get()), old.get(ThaumoryComponents.STORED_ESSENTIA.get()),
                "what the wand held");
        NonNullList<ItemStack> left = recipe.getRemainingItems(input);
        for (int cell : new int[] {0, 3, 5}) {
            ItemStack expected = cell == 0 ? ItemStack.EMPTY : new ItemStack(ThaumoryItems.GOLD_WAND_CAP.get());
            helper.assertTrue(ItemStack.matches(left.get(cell), expected), "left in cell " + cell + ": " + left.get(cell));
        }
        helper.succeed();
    }

    @GameTest
    public void aNewCoreGoesOnAndTheStickComesBack(GameTestHelper helper) {
        CraftingInput input = grid(4, ThaumoryItems.WAND.get(), 1, ThaumoryItems.CRYSTAL_WAND_CORE.get());
        CraftingRecipe recipe = recipe(helper, input).orElseThrow().value();
        helper.assertValueEqual(build(recipe.assemble(input)).core(), Thaumory.id("crystal_wand_core"), "core");
        // The input is trimmed to what is in it, so the core's cell is found rather than assumed.
        int coreCell = input.items().indexOf(input.items().stream().filter(stack -> stack.is(ThaumoryItems.CRYSTAL_WAND_CORE.get())).findFirst()
                .orElseThrow());
        helper.assertTrue(recipe.getRemainingItems(input).get(coreCell).is(Items.STICK), "the stick did not come back");
        helper.succeed();
    }

    @GameTest
    public void theSamePartsChangeNothing(GameTestHelper helper) {
        helper.assertTrue(recipe(helper, grid(0, ThaumoryItems.WAND.get(), 3, ThaumoryItems.GOLD_WAND_CAP.get(), 5,
                ThaumoryItems.GOLD_WAND_CAP.get())).isEmpty(), "gold caps went on a wand with gold caps");
        helper.assertTrue(recipe(helper, grid(0, ThaumoryItems.WAND.get(), 3, Items.STICK)).isEmpty(), "a stick went on a wooden wand");
        helper.succeed();
    }
}
