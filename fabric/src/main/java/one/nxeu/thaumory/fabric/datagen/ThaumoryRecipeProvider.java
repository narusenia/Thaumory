package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.alchemy.AlchemyRecipe;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import net.minecraft.world.item.crafting.Recipe;
import one.nxeu.thaumory.item.ThaumoryItems;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ORDO;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VINCULUM;

/** Crafting table recipes and Crucible alchemy recipes. */
final class ThaumoryRecipeProvider extends FabricRecipeProvider {
    ThaumoryRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
            BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        return new RecipeProvider(recipes, advancements) {
            @Override
            public void buildRecipes() {
                // Lens in a gold rim on a stick handle.
                shaped(RecipeCategory.TOOLS, ThaumoryItems.ARCANE_LOUPE.get())
                        .pattern("NP")
                        .pattern("SN")
                        .define('P', Items.GLASS_PANE)
                        .define('N', Items.GOLD_NUGGET)
                        .define('S', Items.STICK)
                        .unlockedBy(getHasName(Items.GLASS_PANE), has(Items.GLASS_PANE))
                        .save(output);
                shapeless(RecipeCategory.MISC, ThaumoryItems.ARCANE_CODEX.get())
                        .requires(Items.BOOK)
                        .requires(ThaumoryItems.ARCANE_LOUPE.get())
                        .unlockedBy(getHasName(ThaumoryItems.ARCANE_LOUPE.get()), has(ThaumoryItems.ARCANE_LOUPE.get()))
                        .save(output);
                alchemy("blank_rune", Items.STONE, ThaumoryItems.BLANK_RUNE.get(), new AspectStack(ARCANUM, 4), new AspectStack(AQUA, 4));
                alchemy("chalk", Items.CLAY_BALL, ThaumoryItems.CHALK.get(), new AspectStack(ARCANUM, 2), new AspectStack(AQUA, 4));
                alchemy("amplifying_chalk", ThaumoryItems.CHALK.get(), ThaumoryItems.AMPLIFYING_CHALK.get(), new AspectStack(IGNIS, 8));
                alchemy("extending_chalk", ThaumoryItems.CHALK.get(), ThaumoryItems.EXTENDING_CHALK.get(), new AspectStack(AER, 8));
                alchemy("economizing_chalk", ThaumoryItems.CHALK.get(), ThaumoryItems.ECONOMIZING_CHALK.get(), new AspectStack(VINCULUM, 4));
                alchemy("stabilizing_chalk", ThaumoryItems.CHALK.get(), ThaumoryItems.STABILIZING_CHALK.get(), new AspectStack(ORDO, 4));
                shaped(RecipeCategory.BREWING, ThaumoryItems.JAR.get())
                        .pattern(" W ")
                        .pattern("G G")
                        .pattern("GGG")
                        .define('W', ItemTags.WOODEN_SLABS)
                        .define('G', Items.GLASS)
                        .unlockedBy(getHasName(Items.GLASS), has(Items.GLASS))
                        .save(output);
                shapeless(RecipeCategory.MISC, ThaumoryItems.LABEL.get(), 4)
                        .requires(Items.PAPER)
                        .requires(Items.INK_SAC)
                        .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                        .save(output);
                shapeless(RecipeCategory.BREWING, ThaumoryItems.CRUCIBLE.get())
                        .requires(Items.CAULDRON)
                        .requires(Items.GOLD_INGOT)
                        .unlockedBy(getHasName(Items.CAULDRON), has(Items.CAULDRON))
                        .save(output);
            }

            /** Alchemy recipes live under {@code recipe/alchemy/}. They have no advancement: nothing unlocks them in a recipe book. */
            private void alchemy(String name, ItemLike catalyst, ItemLike result, AspectStack... aspects) {
                // Opposite aspects wear each other down while the player gathers them, so a recipe must never need both.
                if (!AspectList.of(aspects).cancellingPairs(ThaumoryApi.aspects()).isEmpty()) {
                    throw new IllegalStateException("Alchemy recipe " + name + " needs aspects that cancel each other out");
                }
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Thaumory.id("alchemy/" + name));
                output.accept(key, new AlchemyRecipe(Ingredient.of(catalyst), AspectList.of(aspects), new ItemStackTemplate(result.asItem())), null);
            }
        };
    }

    @Override
    public String getName() {
        return "Thaumory recipes";
    }
}
