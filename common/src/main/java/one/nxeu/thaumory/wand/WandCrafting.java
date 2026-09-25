package one.nxeu.thaumory.wand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.Identifier;

/**
 * What a crafting grid makes of wand parts (requirements §7.1), by the items' ids alone. The grid
 * comes trimmed to what is in it, row by row, empty cells as empty.
 */
public final class WandCrafting {
    private WandCrafting() {}

    /** What an item can be in a wand recipe. */
    public enum Role { WAND, CAP, CORE, OTHER }

    /** Two of the same caps and a core between them. */
    public record Assembly(Identifier cap, Identifier core) {}

    /**
     * A wand and new parts for it: two of the same caps, or one core.
     *
     * @param wand  the wand's cell
     * @param parts the new parts' cells, where the old parts come back
     */
    public record Rebuild(int wand, Role kind, Identifier part, List<Integer> parts) {}

    /** Two caps and a core in a diagonal line, the core in the middle, and nothing else. */
    public static Optional<Assembly> assembly(int width, int height, List<Optional<Identifier>> cells, Function<Identifier, Role> role) {
        if (width != 3 || height != 3 || cells.stream().filter(Optional::isPresent).count() != 3) {
            return Optional.empty();
        }
        Optional<Identifier> centre = cells.get(4);
        if (centre.isEmpty() || role.apply(centre.get()) != Role.CORE) {
            return Optional.empty();
        }
        for (int[] ends : new int[][] {{0, 8}, {2, 6}}) {
            Optional<Identifier> a = cells.get(ends[0]);
            Optional<Identifier> b = cells.get(ends[1]);
            if (a.isPresent() && a.equals(b) && role.apply(a.get()) == Role.CAP) {
                return Optional.of(new Assembly(a.get(), centre.get()));
            }
        }
        return Optional.empty();
    }

    /** One wand with two of the same caps or one core, anywhere in the grid, and nothing else. */
    public static Optional<Rebuild> rebuild(List<Optional<Identifier>> cells, Function<Identifier, Role> role) {
        int wand = -1;
        List<Integer> parts = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i).isEmpty()) {
                continue;
            }
            Role cell = role.apply(cells.get(i).get());
            if (cell == Role.WAND && wand < 0) {
                wand = i;
            } else if (cell == Role.CAP || cell == Role.CORE) {
                parts.add(i);
            } else {
                return Optional.empty();
            }
        }
        if (wand < 0 || parts.isEmpty()) {
            return Optional.empty();
        }
        Identifier part = cells.get(parts.getFirst()).orElseThrow();
        Role kind = role.apply(part);
        int wanted = kind == Role.CAP ? 2 : 1;
        if (parts.size() != wanted || parts.stream().anyMatch(i -> !cells.get(i).orElseThrow().equals(part))) {
            return Optional.empty();
        }
        return Optional.of(new Rebuild(wand, kind, part, List.copyOf(parts)));
    }
}
