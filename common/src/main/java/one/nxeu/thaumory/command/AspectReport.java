package one.nxeu.thaumory.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.aspect.estimate.AspectEstimation;
import one.nxeu.thaumory.aspect.estimate.AspectEstimator;

/**
 * Lists items without aspects, for writing datapack entries, and recipes that turn aspects into
 * more aspects. Items no readable recipe produces must be written by hand; the rest fill in once
 * their inputs have aspects.
 */
final class AspectReport {
    record Summary(Path file, int withoutRecipe, int unresolved, int gains, int fell) {}

    private record Gain(Identifier recipe, int in, int out) {}

    /** Up to six aspects per item, each rounded down by less than one. */
    private static final int ROUNDING_PER_ITEM = 6;

    private AspectReport() {}

    static Summary write(Path directory) throws IOException {
        List<EstimationRecipe> recipes = AspectEstimation.lastRecipes();
        Set<Identifier> recipeResults = recipes.stream().map(EstimationRecipe::result).collect(Collectors.toSet());
        List<String> withoutRecipe = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (ItemAspects.source(item) != ItemAspects.Source.NONE) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            (recipeResults.contains(id) ? unresolved : withoutRecipe).add(id.toString());
        }
        withoutRecipe.sort(null);
        unresolved.sort(null);
        List<Gain> gains = gains(recipes);

        List<String> lines = new ArrayList<>();
        lines.add("# No readable recipe makes these: give them datapack entries (or {} for none)");
        lines.addAll(withoutRecipe);
        lines.add("");
        lines.add("# Recipes exist, but some input has no aspects yet");
        lines.addAll(unresolved);
        lines.add("");
        lines.add("# Crafting then melting gains aspects (cheapest inputs -> outputs, bonus and rounding excluded)");
        gains.forEach(gain -> lines.add(gain.recipe() + "  " + gain.in() + " -> " + gain.out()));
        lines.add("");
        lines.add("# Fell below half of the first estimate once a cheaper route turned up (first -> final total)");
        Map<Identifier, AspectEstimator.Fall> fell = new TreeMap<>(Comparator.comparing(Identifier::toString));
        fell.putAll(AspectEstimation.lastFell());
        fell.forEach((item, fall) -> lines.add(item + "  " + fall.first() + " -> " + fall.last()));

        Files.createDirectories(directory);
        Path file = directory.resolve("aspect-report.txt");
        Files.write(file, lines);
        return new Summary(file, withoutRecipe.size(), unresolved.size(), gains.size(), fell.size());
    }

    private static List<Gain> gains(List<EstimationRecipe> recipes) {
        List<Gain> gains = new ArrayList<>();
        for (EstimationRecipe recipe : recipes) {
            Optional<Integer> in = cheapestInputs(recipe);
            if (in.isEmpty()) {
                continue;
            }
            int out = (ItemAspects.get(item(recipe.result())).total() - recipe.bonus().total()) * recipe.count();
            // Each published aspect is rounded down by less than one.
            if (out > in.get() + recipe.slots().size() * ROUNDING_PER_ITEM) {
                gains.add(new Gain(recipe.id(), in.get(), out));
            }
        }
        gains.sort(Comparator.comparingInt((Gain g) -> g.out() - g.in()).reversed());
        return gains;
    }

    private static Optional<Integer> cheapestInputs(EstimationRecipe recipe) {
        int total = 0;
        for (List<Identifier> slot : recipe.slots()) {
            int best = Integer.MAX_VALUE;
            for (Identifier option : slot) {
                AspectList value = ItemAspects.get(item(option));
                Optional<Identifier> remainder = AspectEstimation.remainder(option);
                if (remainder.isPresent()) {
                    value = value.minus(ItemAspects.get(item(remainder.get())));
                }
                best = Math.min(best, value.total());
            }
            if (best == Integer.MAX_VALUE) {
                return Optional.empty();
            }
            total += best;
        }
        return Optional.of(total);
    }

    private static Item item(Identifier id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }
}
