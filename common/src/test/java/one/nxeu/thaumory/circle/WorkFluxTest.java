package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorkFluxTest {
    @Test
    void holdsBackLessThanAWholeUnit() {
        WorkFlux.Step step = WorkFlux.add(0.2, 0.1, 0);
        assertEquals(0, step.released());
        assertEquals(0.3, step.held(), 1e-9);
    }

    @Test
    void releasesOnceItComesToOne() {
        WorkFlux.Step step = WorkFlux.add(0.95, 0.1, 0);
        assertEquals(1.05, step.released(), 1e-9);
        assertEquals(0, step.held());
    }

    @Test
    void releasesAtOnceIntoAChunkThatAlreadyHasFlux() {
        assertEquals(new WorkFlux.Step(0.1, 0), WorkFlux.add(0, 0.1, 3));
    }

    @Test
    void nothingGainedNothingReleased() {
        assertEquals(new WorkFlux.Step(0, 0), WorkFlux.add(0, 0, 3));
    }
}
