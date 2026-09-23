package one.nxeu.thaumory.api.estimate;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import one.nxeu.thaumory.api.Experimental;
import one.nxeu.thaumory.api.aspect.AspectList;

/**
 * What aspect estimation needs to know about one recipe: the items that go in and what comes out.
 *
 * @param id recipe id, used to break ties between equally cheap recipes
 * @param slots one entry per consumed item; each entry lists the items that can fill that slot
 * @param result the item produced
 * @param count how many of {@code result} one craft produces
 * @param bonus aspects added per result item, e.g. Ignis for cooking
 */
@Experimental
public record EstimationRecipe(Identifier id, List<List<Identifier>> slots, Identifier result, int count, AspectList bonus) {
    public EstimationRecipe {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(bonus, "bonus");
        slots = slots.stream().map(List::copyOf).toList();
        if (count <= 0) {
            throw new IllegalArgumentException(id + ": result count must be positive, got " + count);
        }
        if (slots.isEmpty() || slots.stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException(id + ": every slot needs at least one item");
        }
    }

    /** One item turning into another in the world, such as copper oxidizing. */
    public static EstimationRecipe worldChange(Identifier id, Identifier from, Identifier to, AspectList bonus) {
        return new EstimationRecipe(id, List.of(List.of(from)), to, 1, bonus);
    }

    /** The item ids an ingredient accepts. */
    public static List<Identifier> slot(Ingredient ingredient) {
        return ingredient.items().map(holder -> BuiltInRegistries.ITEM.getKey(holder.value())).toList();
    }
}
