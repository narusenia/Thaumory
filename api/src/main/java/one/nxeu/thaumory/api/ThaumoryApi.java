package one.nxeu.thaumory.api;

import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.estimate.RecipeAdapterRegistry;

/** Entry point for addons. */
public final class ThaumoryApi {
    public static final String MOD_ID = "thaumory";

    private static final AspectRegistry ASPECTS = new AspectRegistry();
    private static final RecipeAdapterRegistry RECIPE_ADAPTERS = new RecipeAdapterRegistry();

    private ThaumoryApi() {}

    public static AspectRegistry aspects() {
        return ASPECTS;
    }

    /** Recipe types that aspect estimation reads. */
    @Experimental
    public static RecipeAdapterRegistry recipeAdapters() {
        return RECIPE_ADAPTERS;
    }
}
