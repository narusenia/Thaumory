package one.nxeu.thaumory.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import one.nxeu.thaumory.api.text.TextEffect;
import org.junit.jupiter.api.Test;

class TextMotionTest {
    @Test
    void shakeStaysWithinItsAmountAndHoldsForAStep() {
        for (long millis = 0; millis < 5000; millis += 7) {
            for (float x = 0; x < 100; x += 6) {
                assertTrue(Math.abs(TextMotion.offsetX(TextEffect.SHAKE, x, millis)) <= 0.9f);
                assertTrue(Math.abs(TextMotion.offsetY(TextEffect.TREMBLE, x, millis)) <= 0.4f);
            }
        }
        assertEquals(TextMotion.offsetX(TextEffect.SHAKE, 12, 400), TextMotion.offsetX(TextEffect.SHAKE, 12, 439));
        assertNotEquals(TextMotion.offsetX(TextEffect.SHAKE, 12, 400), TextMotion.offsetX(TextEffect.SHAKE, 18, 400));
    }

    @Test
    void neighboursShakeDifferently() {
        Set<Float> offsets = new HashSet<>();
        for (float x = 0; x < 60; x += 6) {
            offsets.add(TextMotion.offsetY(TextEffect.SHAKE, x, 1000));
        }
        assertTrue(offsets.size() > 5);
    }

    @Test
    void onlyMotionEffectsMoveAndOnlyLightEffectsDim() {
        for (TextEffect effect : new TextEffect[] {TextEffect.PULSE, TextEffect.FLICKER, TextEffect.STREAK}) {
            assertEquals(0, TextMotion.offsetX(effect, 5, 1234));
            assertEquals(0, TextMotion.offsetY(effect, 5, 1234));
        }
        for (TextEffect effect : new TextEffect[] {TextEffect.SHAKE, TextEffect.TREMBLE, TextEffect.WAVE, TextEffect.SHIMMER, TextEffect.STREAK}) {
            assertEquals(1, TextMotion.brightness(effect, 5, 1234));
        }
    }

    @Test
    void lightEffectsStayVisible() {
        for (long millis = 0; millis < 5000; millis += 13) {
            float pulse = TextMotion.brightness(TextEffect.PULSE, 3, millis);
            float flicker = TextMotion.brightness(TextEffect.FLICKER, 3, millis);
            assertTrue(pulse >= 0.6f && pulse <= 1);
            assertTrue(flicker >= 0.45f && flicker <= 1);
        }
    }

    @Test
    void waveTravelsAlongTheText() {
        assertNotEquals(TextMotion.offsetY(TextEffect.WAVE, 0, 500), TextMotion.offsetY(TextEffect.WAVE, 8, 500));
        assertTrue(Math.abs(TextMotion.offsetY(TextEffect.WAVE, 0, 777)) <= 1.2f);
    }
}
