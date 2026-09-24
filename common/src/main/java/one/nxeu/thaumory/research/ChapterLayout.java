package one.nxeu.thaumory.research;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * Where each chapter sits in its category's tree (requirements §6.2). A chapter's own {@code x} and
 * {@code y} win; one without them goes to the right of its first required chapter in the same
 * category, or to (0, 0) if there is none, moving down past cells already taken.
 */
public final class ChapterLayout {
    public record Cell(int x, int y) {}

    private ChapterLayout() {}

    /** @param categoryOf each chapter's category, as the book files it */
    public static Map<Identifier, Cell> place(Map<Identifier, Chapter> chapters, Map<Identifier, Identifier> categoryOf) {
        Map<Identifier, Cell> cells = new HashMap<>();
        Map<Identifier, Set<Cell>> taken = new HashMap<>();
        chapters.forEach((id, chapter) -> chapter.position().ifPresent(cell -> {
            cells.put(id, cell);
            taken.computeIfAbsent(categoryOf.get(id), c -> new HashSet<>()).add(cell);
        }));
        // Parents before their children, so a child can sit next to where its parent went.
        Map<Identifier, Integer> depths = new HashMap<>();
        List<Identifier> rest = chapters.keySet().stream().filter(id -> !cells.containsKey(id))
                .sorted(Comparator.<Identifier>comparingInt(id -> depth(id, chapters, depths, new HashSet<>())).thenComparing(id -> id))
                .toList();
        for (Identifier id : rest) {
            Identifier category = categoryOf.get(id);
            Cell start = chapters.get(id).requires().stream()
                    .filter(parent -> category.equals(categoryOf.get(parent)) && cells.containsKey(parent))
                    .findFirst()
                    .map(parent -> new Cell(cells.get(parent).x() + 1, cells.get(parent).y()))
                    .orElse(new Cell(0, 0));
            Set<Cell> used = taken.computeIfAbsent(category, c -> new HashSet<>());
            Cell cell = start;
            while (used.contains(cell)) {
                cell = new Cell(cell.x(), cell.y() + 1);
            }
            used.add(cell);
            cells.put(id, cell);
        }
        return Map.copyOf(cells);
    }

    /** The longest chain of required chapters below {@code id}; a loop counts as ending there. */
    private static int depth(Identifier id, Map<Identifier, Chapter> chapters, Map<Identifier, Integer> depths, Set<Identifier> visiting) {
        Integer known = depths.get(id);
        if (known != null) {
            return known;
        }
        Chapter chapter = chapters.get(id);
        if (chapter == null || !visiting.add(id)) {
            return 0;
        }
        int deepest = 0;
        for (Identifier parent : chapter.requires()) {
            deepest = Math.max(deepest, 1 + depth(parent, chapters, depths, visiting));
        }
        visiting.remove(id);
        depths.put(id, deepest);
        return deepest;
    }
}
