package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrystalPoolTest {
    @Test
    void keepsWhatIsShortOfACrystal() {
        assertEquals(new CrystalPool.Step(0, 9.5), CrystalPool.add(4.5, 5, 10));
    }

    @Test
    void makesACrystalOnceTheresEnough() {
        CrystalPool.Step step = CrystalPool.add(8, 3, 10);
        assertEquals(1, step.crystals());
        assertEquals(1, step.held(), 1e-9);
    }

    @Test
    void makesSeveralFromALargeDraw() {
        assertEquals(new CrystalPool.Step(3, 2), CrystalPool.add(0, 32, 10));
    }

    @Test
    void ignoresANegativeDraw() {
        assertEquals(new CrystalPool.Step(0, 4), CrystalPool.add(4, -1, 10));
    }

    @Test
    void makesNothingWithoutASize() {
        assertEquals(new CrystalPool.Step(0, 12), CrystalPool.add(2, 10, 0));
    }
}
