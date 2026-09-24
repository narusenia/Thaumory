package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ColumnSweepTest {
    @Test
    void startsInTheNorthWestCornerAndRunsEast() {
        ColumnSweep.Step step = ColumnSweep.next(1, 0, 4);
        assertEquals(List.of(new ColumnSweep.Column(-1, -1), new ColumnSweep.Column(0, -1), new ColumnSweep.Column(1, -1),
                new ColumnSweep.Column(-1, 0)), step.columns());
        assertEquals(4, step.cursor());
    }

    @Test
    void wrapsRoundAndCoversEveryColumnOnceARound() {
        Set<ColumnSweep.Column> seen = new HashSet<>();
        int cursor = 0;
        for (int i = 0; i < 9; i++) {
            ColumnSweep.Step step = ColumnSweep.next(4, cursor, 9);
            seen.addAll(step.columns());
            cursor = step.cursor();
        }
        assertEquals(81, seen.size());
        assertEquals(0, cursor);
    }

    @Test
    void neverVisitsAColumnTwiceInOneStep() {
        ColumnSweep.Step step = ColumnSweep.next(1, 5, 100);
        assertEquals(9, step.columns().size());
        assertEquals(9, new HashSet<>(step.columns()).size());
        assertEquals(5, step.cursor());
    }

    @Test
    void aCursorFromAWiderRangeStartsOver() {
        ColumnSweep.Step step = ColumnSweep.next(1, 50, 1);
        assertEquals(List.of(new ColumnSweep.Column(-1, -1)), step.columns());
        assertEquals(1, step.cursor());
    }

    @Test
    void aRangeOfNothingIsTheCoreColumn() {
        assertEquals(List.of(new ColumnSweep.Column(0, 0)), ColumnSweep.next(0, 0, 3).columns());
    }
}
