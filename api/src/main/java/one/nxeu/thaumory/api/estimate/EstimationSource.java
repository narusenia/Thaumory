package one.nxeu.thaumory.api.estimate;

import java.util.Collection;
import one.nxeu.thaumory.api.Experimental;

/**
 * Supplies estimation input that does not come from recipes, such as blocks changing in the world.
 * Called on the server thread each time estimation runs.
 */
@Experimental
@FunctionalInterface
public interface EstimationSource {
    Collection<EstimationRecipe> recipes();
}
