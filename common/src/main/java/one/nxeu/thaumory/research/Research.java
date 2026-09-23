package one.nxeu.thaumory.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/** Every chapter and hint the datapacks define, and how a player moves through them. */
public final class Research {
    public static final Research EMPTY = new Research(Map.of(), Map.of());

    private final Map<Identifier, Chapter> chapters;
    private final Map<Identifier, Hint> hints;
    private final Set<Identifier> gatedRecipes;

    public Research(Map<Identifier, Chapter> chapters, Map<Identifier, Hint> hints) {
        this.chapters = new TreeMap<>(chapters);
        this.hints = new TreeMap<>(hints);
        Set<Identifier> gated = new HashSet<>();
        chapters.values().forEach(chapter -> gated.addAll(chapter.unlocks()));
        this.gatedRecipes = Set.copyOf(gated);
    }

    /** What a player newly completes and newly finds, in the order it happens. */
    public record Advance(List<Identifier> chapters, List<Identifier> hints) {
        public boolean isEmpty() {
            return chapters.isEmpty() && hints.isEmpty();
        }
    }

    public Map<Identifier, Chapter> chapters() {
        return chapters;
    }

    public Map<Identifier, Hint> hints() {
        return hints;
    }

    /** Open once every required chapter is complete. A chapter requiring an unknown one never opens. */
    public boolean isOpen(Identifier id, Set<Identifier> completed) {
        Chapter chapter = chapters.get(id);
        return chapter != null && completed.containsAll(chapter.requires());
    }

    /** Chapters that are open or complete, by {@code order} then id. */
    public List<Identifier> visible(Set<Identifier> completed) {
        return chapters.entrySet().stream()
                .filter(entry -> completed.contains(entry.getKey()) || isOpen(entry.getKey(), completed))
                .sorted(Comparator.<Map.Entry<Identifier, Chapter>>comparingInt(entry -> entry.getValue().order())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** How many defined chapters are not open yet. */
    public int closedCount(Set<Identifier> completed) {
        return chapters.size() - visible(completed).size();
    }

    /**
     * Completes every open chapter whose conditions hold, again and again while completing one opens
     * another that already holds, then finds every hint that holds.
     */
    public Advance advance(ResearchFacts facts, Set<Identifier> completed, Set<Identifier> found) {
        Set<Identifier> done = new LinkedHashSet<>(completed);
        List<Identifier> newChapters = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Identifier id : visible(done)) {
                if (!done.contains(id) && chapters.get(id).met(facts)) {
                    done.add(id);
                    newChapters.add(id);
                    changed = true;
                }
            }
        }
        List<Identifier> newHints = hints.entrySet().stream()
                .filter(entry -> !found.contains(entry.getKey()) && entry.getValue().met(facts))
                .map(Map.Entry::getKey)
                .toList();
        return new Advance(List.copyOf(newChapters), newHints);
    }

    /** Recipes no chapter unlocks are always usable; the rest need a completed chapter that unlocks them. */
    public boolean canUse(Identifier recipe, Set<Identifier> completed) {
        if (!gatedRecipes.contains(recipe)) {
            return true;
        }
        return completed.stream().map(chapters::get).anyMatch(chapter -> chapter != null && chapter.unlocks().contains(recipe));
    }
}
