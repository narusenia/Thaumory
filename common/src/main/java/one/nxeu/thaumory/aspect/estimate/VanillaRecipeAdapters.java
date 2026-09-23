package one.nxeu.thaumory.aspect.estimate;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;
import one.nxeu.thaumory.api.estimate.RecipeAdapterRegistry;
import one.nxeu.thaumory.mixin.ShapedRecipeAccessor;
import one.nxeu.thaumory.mixin.ShapelessRecipeAccessor;
import one.nxeu.thaumory.mixin.SingleItemRecipeAccessor;
import one.nxeu.thaumory.mixin.SmithingTransformRecipeAccessor;
import one.nxeu.thaumory.mixin.TransmuteRecipeAccessor;

/**
 * Adapters for the vanilla recipe types estimation reads: crafting (shaped, shapeless, transmute),
 * cooking (all four), stonecutting and smithing upgrades. Special crafting recipes (fireworks,
 * banner copying, repair...) and brewing are left out on purpose.
 */
public final class VanillaRecipeAdapters {
    private static final AspectList COOKING_BONUS = AspectList.of(IGNIS, 1);

    private VanillaRecipeAdapters() {}

    public static void register(RecipeAdapterRegistry registry) {
        registry.register(ShapedRecipe.class, (id, recipe) ->
                crafting(id, recipe.placementInfo(), ((ShapedRecipeAccessor) recipe).thaumory$result()));
        registry.register(ShapelessRecipe.class, (id, recipe) ->
                crafting(id, recipe.placementInfo(), ((ShapelessRecipeAccessor) recipe).thaumory$result()));
        registry.register(TransmuteRecipe.class, VanillaRecipeAdapters::transmute);
        registry.register(AbstractCookingRecipe.class, (id, recipe) ->
                single(id, recipe.input(), ((SingleItemRecipeAccessor) recipe).thaumory$result(), COOKING_BONUS));
        registry.register(StonecutterRecipe.class, (id, recipe) ->
                single(id, recipe.input(), ((SingleItemRecipeAccessor) recipe).thaumory$result(), AspectList.empty()));
        registry.register(SmithingTransformRecipe.class, VanillaRecipeAdapters::smithing);
    }

    private static Optional<EstimationRecipe> crafting(Identifier id, PlacementInfo placement, ItemStackTemplate result) {
        if (placement.isImpossibleToPlace()) {
            return Optional.empty();
        }
        return recipe(id, placement.ingredients(), result.item(), result.count(), AspectList.empty());
    }

    /** The result keeps the input's components, but its item is fixed unless the recipe leaves it unset. */
    private static Optional<EstimationRecipe> transmute(Identifier id, TransmuteRecipe recipe) {
        PlacementInfo placement = recipe.placementInfo();
        TransmuteResult result = ((TransmuteRecipeAccessor) recipe).thaumory$result();
        if (placement.isImpossibleToPlace() || result.item().isEmpty()) {
            return Optional.empty();
        }
        return recipe(id, placement.ingredients(), result.item().get(), result.count(), AspectList.empty());
    }

    private static Optional<EstimationRecipe> single(Identifier id, Ingredient input, ItemStackTemplate result, AspectList bonus) {
        return recipe(id, List.of(input), result.item(), result.count(), bonus);
    }

    private static Optional<EstimationRecipe> smithing(Identifier id, SmithingTransformRecipe recipe) {
        List<Ingredient> inputs = new ArrayList<>();
        recipe.templateIngredient().ifPresent(inputs::add);
        inputs.add(recipe.baseIngredient());
        recipe.additionIngredient().ifPresent(inputs::add);
        ItemStackTemplate result = ((SmithingTransformRecipeAccessor) recipe).thaumory$result();
        return recipe(id, inputs, result.item(), result.count(), AspectList.empty());
    }

    private static Optional<EstimationRecipe> recipe(
            Identifier id, List<Ingredient> inputs, Holder<Item> result, int count, AspectList bonus) {
        List<List<Identifier>> slots = inputs.stream().map(EstimationRecipe::slot).toList();
        if (slots.isEmpty() || slots.stream().anyMatch(List::isEmpty) || count <= 0) {
            return Optional.empty();
        }
        return Optional.of(new EstimationRecipe(id, slots, BuiltInRegistries.ITEM.getKey(result.value()), count, bonus));
    }
}
