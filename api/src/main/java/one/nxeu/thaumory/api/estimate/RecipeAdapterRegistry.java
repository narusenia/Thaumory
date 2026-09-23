package one.nxeu.thaumory.api.estimate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import one.nxeu.thaumory.api.Experimental;

/**
 * Recipe types that aspect estimation understands. A recipe uses the adapter registered for its
 * exact class, or else for its nearest registered superclass. Recipes with no adapter are ignored.
 */
@Experimental
public final class RecipeAdapterRegistry {
    private final Map<Class<?>, RecipeAdapter<?>> adapters = new LinkedHashMap<>();

    public <R extends Recipe<?>> void register(Class<R> type, RecipeAdapter<? super R> adapter) {
        if (adapters.putIfAbsent(type, adapter) != null) {
            throw new IllegalArgumentException("A recipe adapter for " + type.getName() + " is already registered");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<EstimationRecipe> adapt(Identifier id, Recipe<?> recipe) {
        for (Class<?> type = recipe.getClass(); type != null; type = type.getSuperclass()) {
            RecipeAdapter adapter = adapters.get(type);
            if (adapter != null) {
                return adapter.adapt(id, recipe);
            }
        }
        return Optional.empty();
    }
}
