package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import one.nxeu.thaumory.item.ThaumoryItems;

/** Crafting table recipes. Alchemy recipes are a separate datapack type (M1-15). */
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
                shapeless(RecipeCategory.BREWING, ThaumoryItems.CRUCIBLE.get())
                        .requires(Items.CAULDRON)
                        .requires(Items.GOLD_INGOT)
                        .unlockedBy(getHasName(Items.CAULDRON), has(Items.CAULDRON))
                        .save(output);
            }
        };
    }

    @Override
    public String getName() {
        return "Thaumory recipes";
    }
}
