package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks the columns of a circle's range in order, a few at a time, so every column comes round
 * again before any is seen twice (requirements §17.4). The cursor is the index of the next column,
 * kept with the Core between calls.
 */
public final class ColumnSweep {
    private ColumnSweep() {}

    /** A column's offset from the Core, east ({@code dx}) and south ({@code dz}). */
    public record Column(int dx, int dz) {}

    /** The columns visited this time and where the next call carries on. */
    public record Step(List<Column> columns, int cursor) {}

    /**
     * The next {@code count} columns of the square reaching {@code radius} out from the Core,
     * starting at {@code cursor}. Rows run west to east, north to south, and wrap round at the end.
     * A cursor left over from a wider range starts again from the corner. No column comes up
     * twice in one step.
     */
    public static Step next(int radius, int cursor, int count) {
        int side = 2 * Math.max(0, radius) + 1;
        int total = side * side;
        int start = cursor >= 0 && cursor < total ? cursor : 0;
        int visits = Math.min(Math.max(0, count), total);
        List<Column> columns = new ArrayList<>(visits);
        for (int i = 0; i < visits; i++) {
            int index = (start + i) % total;
            columns.add(new Column(index % side - radius, index / side - radius));
        }
        return new Step(columns, (start + visits) % total);
    }
}
