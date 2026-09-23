package one.nxeu.thaumory.api.estimate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import one.nxeu.thaumory.api.Experimental;

/**
 * What aspect estimation reads. A recipe uses the adapter registered for its exact class, or else
 * for its nearest registered superclass; recipes with no adapter are ignored. Sources add
 * transformations that are not recipes, such as copper oxidizing.
 */
@Experimental
public final class RecipeAdapterRegistry {
    private final Map<Class<?>, RecipeAdapter<?>> adapters = new LinkedHashMap<>();
    private final List<EstimationSource> sources = new ArrayList<>();

    public <R extends Recipe<?>> void register(Class<R> type, RecipeAdapter<? super R> adapter) {
        if (adapters.putIfAbsent(type, adapter) != null) {
            throw new IllegalArgumentException("A recipe adapter for " + type.getName() + " is already registered");
        }
    }

    public void registerSource(EstimationSource source) {
        sources.add(source);
    }

    /** Every registered source's recipes, collected now. */
    public List<EstimationRecipe> sourceRecipes() {
        List<EstimationRecipe> recipes = new ArrayList<>();
        sources.forEach(source -> recipes.addAll(source.recipes()));
        return recipes;
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
