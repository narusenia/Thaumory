package one.nxeu.thaumory.aspect.estimate;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.CHAOS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.MORS;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;
import one.nxeu.thaumory.api.estimate.RecipeAdapterRegistry;

/**
 * Vanilla items made by something happening in the world rather than by a recipe, each adding what
 * the world contributed (water, time, wear).
 */
public final class VanillaWorldChanges {
    private VanillaWorldChanges() {}

    public static void register(RecipeAdapterRegistry registry) {
        registry.registerSource(VanillaWorldChanges::recipes);
    }

    static List<EstimationRecipe> recipes() {
        List<EstimationRecipe> recipes = new ArrayList<>();

        // Copper weathers in the air and rain.
        WeatheringCopper.NEXT_BY_BLOCK.get().forEach((from, to) -> add(recipes, "oxidation", from, to, AQUA));

        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals("minecraft")) {
                continue;
            }
            String path = id.getPath();
            // Concrete powder sets in water.
            if (path.endsWith("_concrete_powder")) {
                add(recipes, "concrete", id, id.withPath(path.replace("_concrete_powder", "_concrete")), AQUA);
            }
            // Coral dies out of water.
            if (path.contains("coral") && !path.startsWith("dead_")) {
                add(recipes, "coral_death", id, id.withPath("dead_" + path), MORS);
            }
        }

        // Anvils wear down with use.
        for (Block anvil : List.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL)) {
            BlockState damaged = AnvilBlock.damage(anvil.defaultBlockState());
            if (damaged != null) {
                add(recipes, "anvil_damage", anvil, damaged.getBlock(), CHAOS);
            }
        }

        add(recipes, "carving", key(Items.PUMPKIN), key(Items.CARVED_PUMPKIN), null);
        add(recipes, "wetting", key(Items.DIRT), key(Items.MUD), AQUA);
        return recipes;
    }

    private static void add(List<EstimationRecipe> recipes, String kind, Block from, Block to, Aspect bonus) {
        Item fromItem = from.asItem();
        Item toItem = to.asItem();
        if (fromItem != Items.AIR && toItem != Items.AIR) {
            add(recipes, kind, key(fromItem), key(toItem), bonus);
        }
    }

    private static void add(List<EstimationRecipe> recipes, String kind, Identifier from, Identifier to, Aspect bonus) {
        if (!BuiltInRegistries.ITEM.containsKey(to)) {
            return;
        }
        Identifier id = Thaumory.id("world/" + kind + "/" + to.getPath());
        recipes.add(EstimationRecipe.worldChange(id, from, to, bonus == null ? AspectList.empty() : AspectList.of(bonus, 1)));
    }

    private static Identifier key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
