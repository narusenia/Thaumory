package one.nxeu.thaumory.aspect.estimate;

import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.LifecycleEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import org.slf4j.Logger;

/**
 * Runs {@link AspectEstimator} over the server's recipes when the server starts and after every
 * datapack reload, once recipes and tags are both in place.
 */
public final class AspectEstimation {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile MinecraftServer server;

    private AspectEstimation() {}

    public static void registerEvents() {
        LifecycleEvent.SERVER_STARTING.register(s -> server = s);
        LifecycleEvent.SERVER_STOPPED.register(s -> server = null);
        LifecycleEvent.SERVER_STARTED.register(AspectEstimation::run);
        LifecycleEvent.TAGS_UPDATED.register((registries, client) -> {
            if (client) {
                return;
            }
            // Tag membership decides datapack lookups, so drop anything cached before tags bound.
            ItemAspects.invalidate();
            MinecraftServer current = server;
            if (current != null) {
                // Wait for the reload to finish so the new recipe manager is in place.
                current.execute(() -> run(current));
            }
        });
    }

    static void run(MinecraftServer server) {
        long start = System.nanoTime();

        Map<Identifier, AspectList> manual = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemAspects.manual(item).ifPresent(aspects -> manual.put(BuiltInRegistries.ITEM.getKey(item), aspects));
        }

        List<EstimationRecipe> recipes = new ArrayList<>();
        int skipped = 0;
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            Identifier id = holder.id().identifier();
            try {
                Optional<EstimationRecipe> recipe = ThaumoryApi.recipeAdapters().adapt(id, holder.value());
                recipe.ifPresent(recipes::add);
            } catch (RuntimeException e) {
                skipped++;
                LOGGER.warn("Could not read recipe {} for aspect estimation", id, e);
            }
        }

        AspectEstimator.Result result = AspectEstimator.estimate(manual, recipes, AspectEstimation::remainder);
        ItemAspects.updateEstimated(result.estimated());

        LOGGER.info("Estimated aspects for {} items from {} recipes in {} rounds ({} ms); {} items stayed unresolved, {} recipes failed",
                result.estimated().size(), recipes.size(), result.rounds(),
                (System.nanoTime() - start) / 1_000_000, result.unresolved().size(), skipped);
        if (!result.unresolved().isEmpty()) {
            LOGGER.debug("Unresolved items: {}", result.unresolved());
        }
    }

    private static Optional<Identifier> remainder(Identifier item) {
        ItemStackTemplate remainder = BuiltInRegistries.ITEM.getValue(item).getCraftingRemainder();
        return Optional.ofNullable(remainder).map(template -> BuiltInRegistries.ITEM.getKey(template.item().value()));
    }
}
