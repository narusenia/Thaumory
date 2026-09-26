package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.alchemy.AlchemyRecipe;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.item.EquipmentSet;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.wand.WandAssemblyRecipe;
import one.nxeu.thaumory.wand.WandRebuildRecipe;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARTIFICIUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BELLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ORDO;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TEMPESTAS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
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
                // A crystal lens in a gold rim on a stick handle.
                shaped(RecipeCategory.TOOLS, ThaumoryItems.ARCANE_LOUPE.get())
                        .pattern("NP")
                        .pattern("SN")
                        .define('P', ThaumoryItems.ARCANE_CRYSTAL_SHARD.get())
                        .define('N', Items.GOLD_NUGGET)
                        .define('S', Items.STICK)
                        .unlockedBy(getHasName(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get()), has(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get()))
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
                alchemy("blank_scroll", Items.PAPER, ThaumoryItems.BLANK_SCROLL.get(), new AspectStack(ARCANUM, 2));
                alchemy("arcane_iron_ingot", Items.IRON_INGOT, ThaumoryItems.ARCANE_IRON.ingot().get(), new AspectStack(ARCANUM, 8));
                alchemy("aether_silver_ingot", ThaumoryItems.ARCANE_IRON.ingot().get(), ThaumoryItems.AETHER_SILVER.ingot().get(),
                        new AspectStack(AER, 16), new AspectStack(LUX, 16));
                alchemy("monocle", ThaumoryItems.ARCANE_LOUPE.get(), ThaumoryItems.MONOCLE.get(), new AspectStack(LUX, 12), new AspectStack(ARCANUM, 8));
                alchemy("crystal_wand_core", Items.STICK, ThaumoryItems.CRYSTAL_WAND_CORE.get(), new AspectStack(ARCANUM, 8), new AspectStack(LUX, 4));
                alchemy("light_focus", ThaumoryItems.BLANK_FOCUS.get(), ThaumoryItems.LIGHT_FOCUS.get(), new AspectStack(LUX, 12));
                alchemy("fire_focus", ThaumoryItems.BLANK_FOCUS.get(), ThaumoryItems.FIRE_FOCUS.get(), new AspectStack(IGNIS, 12));
                alchemy("frost_focus", ThaumoryItems.BLANK_FOCUS.get(), ThaumoryItems.FROST_FOCUS.get(), new AspectStack(AQUA, 12));
                alchemy("lightning_focus", ThaumoryItems.BLANK_FOCUS.get(), ThaumoryItems.LIGHTNING_FOCUS.get(), new AspectStack(TEMPESTAS, 12));
                alchemy("digging_focus", ThaumoryItems.BLANK_FOCUS.get(), ThaumoryItems.DIGGING_FOCUS.get(),
                        new AspectStack(TERRA, 8), new AspectStack(BELLUM, 8));
                alchemy("leap_focus", ThaumoryItems.BLANK_FOCUS.get(), ThaumoryItems.LEAP_FOCUS.get(), new AspectStack(ARCANUM, 8), new AspectStack(AER, 8));
                // Ordo and Chaos, what the focus pays with, would cancel out in the Crucible.
                alchemy("exchange_focus", ThaumoryItems.BLANK_FOCUS.get(), ThaumoryItems.EXCHANGE_FOCUS.get(),
                        new AspectStack(ARTIFICIUM, 8), new AspectStack(ORDO, 4));
                // A crystal shard held in a ring of gold (requirements §17.7).
                shaped(RecipeCategory.TOOLS, ThaumoryItems.BLANK_FOCUS.get())
                        .pattern(" N ")
                        .pattern("NCN")
                        .pattern(" N ")
                        .define('N', Items.GOLD_NUGGET)
                        .define('C', ThaumoryItems.ARCANE_CRYSTAL_SHARD.get())
                        .unlockedBy(getHasName(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get()), has(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get()))
                        .save(output);
                // A gold chain with an Arcane Iron charm hanging from it.
                shaped(RecipeCategory.TOOLS, ThaumoryItems.AMULET.get())
                        .pattern("N N")
                        .pattern("N N")
                        .pattern(" I ")
                        .define('N', Items.GOLD_NUGGET)
                        .define('I', ThaumoryItems.ARCANE_IRON.ingot().get())
                        .unlockedBy(getHasName(ThaumoryItems.ARCANE_IRON.ingot().get()), has(ThaumoryItems.ARCANE_IRON.ingot().get()))
                        .save(output);
                equipment(ThaumoryItems.ARCANE_IRON);
                equipment(ThaumoryItems.AETHER_SILVER);
                shaped(RecipeCategory.BREWING, ThaumoryItems.PIPE.get(), 8)
                        .pattern("NGN")
                        .pattern("NGN")
                        .pattern("NGN")
                        .define('N', Items.GOLD_NUGGET)
                        .define('G', Items.GLASS)
                        .unlockedBy(getHasName(Items.GLASS), has(Items.GLASS))
                        .save(output);
                shapeless(RecipeCategory.BREWING, ThaumoryItems.FILTER_PIPE.get())
                        .requires(ThaumoryItems.PIPE.get())
                        .requires(Items.GOLD_INGOT)
                        .unlockedBy(getHasName(ThaumoryItems.PIPE.get()), has(ThaumoryItems.PIPE.get()))
                        .save(output);
                shapeless(RecipeCategory.BREWING, ThaumoryItems.VALVE.get())
                        .requires(ThaumoryItems.PIPE.get())
                        .requires(Items.LEVER)
                        .unlockedBy(getHasName(ThaumoryItems.PIPE.get()), has(ThaumoryItems.PIPE.get()))
                        .save(output);
                shapeless(RecipeCategory.BREWING, ThaumoryItems.PUMP.get())
                        .requires(ThaumoryItems.PIPE.get())
                        .requires(Items.PISTON)
                        .unlockedBy(getHasName(ThaumoryItems.PIPE.get()), has(ThaumoryItems.PIPE.get()))
                        .save(output);
                shaped(RecipeCategory.MISC, ThaumoryItems.PEDESTAL.get())
                        .pattern("SSS")
                        .pattern(" G ")
                        .define('S', Items.STONE_BRICK_SLAB)
                        .define('G', Items.GOLD_INGOT)
                        .unlockedBy(getHasName(ThaumoryItems.CIRCLE_CORE.get()), has(ThaumoryItems.CIRCLE_CORE.get()))
                        .save(output);
                shaped(RecipeCategory.BREWING, ThaumoryItems.JAR.get())
                        .pattern(" W ")
                        .pattern("G G")
                        .pattern("GGG")
                        .define('W', ItemTags.WOODEN_SLABS)
                        .define('G', Items.GLASS)
                        .unlockedBy(getHasName(Items.GLASS), has(Items.GLASS))
                        .save(output);
                // A wand is two of the same caps and a core in a diagonal line, whatever the parts (requirements §7.1).
                SpecialRecipeBuilder.special(() -> WandAssemblyRecipe.INSTANCE).save(output, "wand_assembly");
                SpecialRecipeBuilder.special(() -> WandRebuildRecipe.INSTANCE).save(output, "wand_rebuild");
                shapeless(RecipeCategory.TOOLS, ThaumoryItems.GOLD_WAND_CAP.get())
                        .requires(Items.GOLD_NUGGET, 2)
                        .unlockedBy(getHasName(Items.GOLD_NUGGET), has(Items.GOLD_NUGGET))
                        .save(output);
                shapeless(RecipeCategory.TOOLS, ThaumoryItems.ARCANE_IRON_WAND_CAP.get())
                        .requires(ThaumoryItems.ARCANE_IRON.ingot().get())
                        .requires(Items.GOLD_NUGGET)
                        .unlockedBy(getHasName(ThaumoryItems.ARCANE_IRON.ingot().get()), has(ThaumoryItems.ARCANE_IRON.ingot().get()))
                        .save(output);
                shapeless(RecipeCategory.TOOLS, ThaumoryItems.AETHER_SILVER_WAND_CAP.get())
                        .requires(ThaumoryItems.AETHER_SILVER.ingot().get())
                        .requires(Items.GOLD_NUGGET)
                        .unlockedBy(getHasName(ThaumoryItems.AETHER_SILVER.ingot().get()), has(ThaumoryItems.AETHER_SILVER.ingot().get()))
                        .save(output);
                shaped(RecipeCategory.MISC, ThaumoryItems.CIRCLE_CORE.get())
                        .pattern("SGS")
                        .pattern("GRG")
                        .pattern("SGS")
                        .define('S', Items.STONE_BRICKS)
                        .define('G', Items.GOLD_INGOT)
                        .define('R', ThaumoryItems.BLANK_RUNE.get())
                        .unlockedBy(getHasName(ThaumoryItems.BLANK_RUNE.get()), has(ThaumoryItems.BLANK_RUNE.get()))
                        .save(output);
                shaped(RecipeCategory.MISC, ThaumoryItems.BLANK_CIRCLE_STONE.get())
                        .pattern("SCS")
                        .pattern("SIS")
                        .pattern("SSS")
                        .define('S', Items.STONE_BRICKS)
                        .define('C', ThaumoryItems.ARCANE_CRYSTAL_SHARD.get())
                        .define('I', ThaumoryItems.ARCANE_IRON.ingot().get())
                        .unlockedBy(getHasName(ThaumoryItems.ARCANE_IRON.ingot().get()), has(ThaumoryItems.ARCANE_IRON.ingot().get()))
                        .save(output);
                // Each rank: the Core one below it, ringed by its metal (requirements §4.6).
                coreUpgrade(ThaumoryItems.CIRCLE_CORE.get(), ThaumoryItems.ARCANE_IRON.ingot().get(), ThaumoryItems.ARCANE_IRON_CIRCLE_CORE.get());
                coreUpgrade(ThaumoryItems.ARCANE_IRON_CIRCLE_CORE.get(), ThaumoryItems.AETHER_SILVER.ingot().get(),
                        ThaumoryItems.AETHER_SILVER_CIRCLE_CORE.get());
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

            /** A Core of the next rank: {@code core} in the middle, {@code ingot} on each side and stone bricks in the corners. */
            private void coreUpgrade(Item core, Item ingot, Item result) {
                shaped(RecipeCategory.MISC, result)
                        .pattern("SIS")
                        .pattern("ICI")
                        .pattern("SIS")
                        .define('S', Items.STONE_BRICKS)
                        .define('I', ingot)
                        .define('C', core)
                        .unlockedBy(getHasName(ingot), has(ingot))
                        .save(output);
            }

            /** The gear in vanilla's shapes, with sticks for handles. */
            private void equipment(EquipmentSet set) {
                Item ingot = set.ingot().get();
                gear(RecipeCategory.COMBAT, set.sword().get(), ingot, " I ", " I ", " S ");
                gear(RecipeCategory.TOOLS, set.pickaxe().get(), ingot, "III", " S ", " S ");
                gear(RecipeCategory.TOOLS, set.axe().get(), ingot, "II", "IS", " S");
                gear(RecipeCategory.TOOLS, set.shovel().get(), ingot, "I", "S", "S");
                gear(RecipeCategory.TOOLS, set.hoe().get(), ingot, "II", " S", " S");
                gear(RecipeCategory.COMBAT, set.helmet().get(), ingot, "III", "I I");
                gear(RecipeCategory.COMBAT, set.chestplate().get(), ingot, "I I", "III", "III");
                gear(RecipeCategory.COMBAT, set.leggings().get(), ingot, "III", "I I", "I I");
                gear(RecipeCategory.COMBAT, set.boots().get(), ingot, "I I", "I I");
            }

            private void gear(RecipeCategory category, Item result, Item ingot, String... rows) {
                var recipe = shaped(category, result);
                for (String row : rows) {
                    recipe.pattern(row);
                }
                recipe.define('I', ingot);
                if (String.join("", rows).contains("S")) {
                    recipe.define('S', Items.STICK);
                }
                recipe.unlockedBy(getHasName(ingot), has(ingot)).save(output);
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
