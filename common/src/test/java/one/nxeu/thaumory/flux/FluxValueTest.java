package one.nxeu.thaumory.flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FluxValueTest {
    private static final FluxSettings SETTINGS = FluxSettings.DEFAULT;
    private static final long HOUR = 72_000;

    @Test
    void decaysFromWhenItWasStored() {
        FluxValue value = new FluxValue(100, 1_000);

        assertEquals(100, value.amountAt(1_000, SETTINGS));
        assertEquals(90, value.amountAt(1_000 + HOUR, SETTINGS), 1e-9);
    }

    @Test
    void dropsToZeroBelowOne() {
        FluxValue value = new FluxValue(1.05, 0);

        assertEquals(1.05, value.amountAt(0, SETTINGS));
        assertEquals(0, value.amountAt(HOUR, SETTINGS));
    }

    @Test
    void doesNotKeepAmountsBelowOne() {
        assertTrue(FluxValue.of(0.99, 0).isEmpty());
        assertTrue(FluxValue.of(0, 0).isEmpty());
        assertEquals(Optional.of(new FluxValue(1, 5)), FluxValue.of(1, 5));
    }

    @Test
    void readsNoValueAsZero() {
        assertEquals(0, FluxValue.amountAt(Optional.empty(), 100, SETTINGS));
    }

    @Test
    void addingRestartsDecayFromTheDecayedAmount() {
        FluxValue first = FluxValue.of(100, 0).orElseThrow();
        long now = HOUR;
        FluxValue second = FluxValue.of(first.amountAt(now, SETTINGS) + 10, now).orElseThrow();

        assertEquals(100, second.amountAt(now, SETTINGS), 1e-9);
        assertEquals(90, second.amountAt(now + HOUR, SETTINGS), 1e-9);
    }

    @Test
    void roundTripsThroughCodec() {
        FluxValue value = new FluxValue(12.5, 123_456_789_012L);
        var json = FluxValue.CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        assertEquals(value, FluxValue.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
