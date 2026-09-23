package one.nxeu.thaumory.api;

import java.util.Objects;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.circle.CircleEffectRegistry;
import one.nxeu.thaumory.api.estimate.RecipeAdapterRegistry;
import one.nxeu.thaumory.api.flux.ChunkFlux;

/** Entry point for addons. */
public final class ThaumoryApi {
    public static final String MOD_ID = "thaumory";

    private static final AspectRegistry ASPECTS = new AspectRegistry();
    private static final RecipeAdapterRegistry RECIPE_ADAPTERS = new RecipeAdapterRegistry();
    private static final CircleEffectRegistry CIRCLE_EFFECTS = new CircleEffectRegistry();
    private static volatile ChunkFlux flux;

    private ThaumoryApi() {}

    public static AspectRegistry aspects() {
        return ASPECTS;
    }

    /**
     * What magic circles do. Blocks count as circle patterns when they are in the block tag
     * {@code thaumory:circle_patterns}; their modifier values go in {@code circle.json}.
     */
    public static CircleEffectRegistry circleEffects() {
        return CIRCLE_EFFECTS;
    }

    /** Recipe types that aspect estimation reads. */
    @Experimental
    public static RecipeAdapterRegistry recipeAdapters() {
        return RECIPE_ADAPTERS;
    }

    /**
     * Flux in each chunk.
     *
     * @throws IllegalStateException before Thaumory has initialized
     */
    public static ChunkFlux flux() {
        ChunkFlux current = flux;
        if (current == null) {
            throw new IllegalStateException("Thaumory has not initialized yet");
        }
        return current;
    }

    /** Called once by Thaumory itself during initialization. Addons must not call this. */
    public static void provideFlux(ChunkFlux implementation) {
        Objects.requireNonNull(implementation, "implementation");
        synchronized (ThaumoryApi.class) {
            if (flux != null) {
                throw new IllegalStateException("Flux is already provided");
            }
            flux = implementation;
        }
    }
}
