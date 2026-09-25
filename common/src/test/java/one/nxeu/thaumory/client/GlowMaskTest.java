package one.nxeu.thaumory.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlowMaskTest {
    @Test
    void addsTheMarginOnEverySide() {
        assertEquals(10 * 10, GlowMask.of(new int[4 * 4], 4, 4, 3, 1).length);
    }

    @Test
    void nothingStaysNothing() {
        assertArrayEquals(new int[6 * 6], GlowMask.of(new int[2 * 2], 2, 2, 2, 1));
    }

    @Test
    void aDotSpreadsIntoTheMarginAndFadesOutward() {
        int[] dot = new int[3 * 3];
        dot[4] = 255;
        int[] glow = GlowMask.of(dot, 3, 3, 3, 1);
        int w = 9;
        int centre = glow[4 * w + 4];
        int near = glow[4 * w + 5];
        int far = glow[4 * w + 6];
        assertTrue(centre > near && near > far && far > 0, centre + " " + near + " " + far);
        assertEquals(0, glow[0]);
        // Symmetric around the dot.
        assertEquals(glow[4 * w + 3], near);
        assertEquals(glow[3 * w + 4], near);
    }

    @Test
    void neverGoesPastFull() {
        int[] full = new int[4 * 4];
        java.util.Arrays.fill(full, 255);
        for (int value : GlowMask.of(full, 4, 4, 1, 1)) {
            assertTrue(value >= 0 && value <= 255);
        }
    }
}
