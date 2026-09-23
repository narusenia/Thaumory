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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.network.AspectSync;
import org.slf4j.Logger;

/**
 * Runs {@link AspectEstimator} over the server's recipes when the server starts and after every
 * datapack reload, once recipes and tags are both in place.
 */
public final class AspectEstimation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AspectList LIGHT_BONUS = AspectList.of(ThaumoryAspects.LUX, 1);

    private static volatile MinecraftServer server;
    private static volatile List<EstimationRecipe> lastRecipes = List.of();
    private static volatile Map<Identifier, AspectEstimator.Fall> lastFell = Map.of();

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

        try {
            recipes.addAll(ThaumoryApi.recipeAdapters().sourceRecipes());
        } catch (RuntimeException e) {
            LOGGER.warn("An aspect estimation source failed", e);
        }

        recipes = new ArrayList<>(AspectEstimator.withBonus(recipes, AspectEstimation::givesOffLight, LIGHT_BONUS));
        AspectEstimator.Result result = AspectEstimator.estimate(manual, recipes, AspectEstimation::remainder);
        lastRecipes = List.copyOf(recipes);
        lastFell = result.fell();
        ItemAspects.updateEstimated(result.estimated());
        AspectSync.sendToAll(server);

        LOGGER.info("Estimated aspects for {} items from {} recipes in {} rounds, {} lowered by cheaper routes ({} ms); {} items stayed unresolved, {} recipes failed",
                result.estimated().size(), recipes.size(), result.rounds(), result.lowered(),
                (System.nanoTime() - start) / 1_000_000, result.unresolved().size(), skipped);
        if (!result.unresolved().isEmpty()) {
            LOGGER.debug("Unresolved items: {}", result.unresolved());
        }
    }

    /** Every recipe and world change the last run read. */
    public static List<EstimationRecipe> lastRecipes() {
        return lastRecipes;
    }

    /** Items whose value fell below half of what they first settled at, as of the last run. */
    public static Map<Identifier, AspectEstimator.Fall> lastFell() {
        return lastFell;
    }

    /** A block item whose block, placed as it comes, gives off light. */
    private static boolean givesOffLight(Identifier item) {
        return BuiltInRegistries.ITEM.getValue(item) instanceof BlockItem block
                && block.getBlock().defaultBlockState().getLightEmission() > 0;
    }

    public static Optional<Identifier> remainder(Identifier item) {
        ItemStackTemplate remainder = BuiltInRegistries.ITEM.getValue(item).getCraftingRemainder();
        return Optional.ofNullable(remainder).map(template -> BuiltInRegistries.ITEM.getKey(template.item().value()));
    }
}
