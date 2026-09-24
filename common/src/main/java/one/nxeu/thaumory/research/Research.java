package one.nxeu.thaumory.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

/** Every chapter, hint and category the datapacks define, and how a player moves through them. */
public final class Research {
    public static final Research EMPTY = new Research(Map.of(), Map.of(), Map.of());

    private final Map<Identifier, Chapter> chapters;
    private final Map<Identifier, Hint> hints;
    private final Map<Identifier, Category> categories;
    /** Each chapter's category; one naming a category no datapack defines goes to {@link Chapter#BASICS}. */
    private final Map<Identifier, Identifier> categoryOf;
    private final Map<Identifier, ChapterLayout.Cell> cells;
    private final Set<Identifier> gatedRecipes;

    public Research(Map<Identifier, Chapter> chapters, Map<Identifier, Hint> hints, Map<Identifier, Category> categories) {
        this.chapters = new TreeMap<>(chapters);
        this.hints = new TreeMap<>(hints);
        this.categories = new TreeMap<>(categories);
        Map<Identifier, Identifier> filed = new HashMap<>();
        chapters.forEach((id, chapter) -> filed.put(id, categories.containsKey(chapter.category()) ? chapter.category() : Chapter.BASICS));
        this.categoryOf = Map.copyOf(filed);
        this.cells = ChapterLayout.place(chapters, categoryOf);
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

    public Map<Identifier, Category> categories() {
        return categories;
    }

    public Identifier categoryOf(Identifier chapter) {
        return categoryOf.getOrDefault(chapter, Chapter.BASICS);
    }

    public ChapterLayout.Cell cell(Identifier chapter) {
        return cells.getOrDefault(chapter, new ChapterLayout.Cell(0, 0));
    }

    /** Open once every required chapter is complete. A chapter requiring an unknown one never opens. */
    public boolean isOpen(Identifier id, Set<Identifier> completed) {
        Chapter chapter = chapters.get(id);
        return chapter != null && completed.containsAll(chapter.requires());
    }

    /** Chapters that are open or complete, by id. */
    public List<Identifier> visible(Set<Identifier> completed) {
        return chapters.keySet().stream().filter(id -> completed.contains(id) || isOpen(id, completed)).toList();
    }

    /** Chapters not open yet, but with at least one required chapter complete: the book marks them "?". */
    public List<Identifier> unknown(Set<Identifier> completed) {
        return chapters.entrySet().stream()
                .filter(entry -> !completed.contains(entry.getKey()) && !isOpen(entry.getKey(), completed))
                .filter(entry -> entry.getValue().requires().stream().anyMatch(completed::contains))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Categories with an open or "?" chapter, by {@code order} then id. */
    public List<Identifier> shownCategories(Set<Identifier> completed) {
        Set<Identifier> used = new HashSet<>();
        visible(completed).forEach(id -> used.add(categoryOf(id)));
        unknown(completed).forEach(id -> used.add(categoryOf(id)));
        return categories.entrySet().stream()
                .filter(entry -> used.contains(entry.getKey()))
                .sorted(Comparator.<Map.Entry<Identifier, Category>>comparingInt(entry -> entry.getValue().order())
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .toList();
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
