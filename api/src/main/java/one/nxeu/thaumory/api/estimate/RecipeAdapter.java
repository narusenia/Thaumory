package one.nxeu.thaumory.api.estimate;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import one.nxeu.thaumory.api.Experimental;

/**
 * Reads a recipe type for aspect estimation. Register one for your mod's recipe types with
 * {@link RecipeAdapterRegistry#register(Class, RecipeAdapter)}.
 */
@Experimental
@FunctionalInterface
public interface RecipeAdapter<R extends Recipe<?>> {
    /** Returns empty when this particular recipe cannot be estimated (e.g. its output is dynamic). */
    Optional<EstimationRecipe> adapt(Identifier id, R recipe);
}
