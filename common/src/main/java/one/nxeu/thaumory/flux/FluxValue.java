package one.nxeu.thaumory.flux;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * The Flux stored on a chunk: the amount as of game tick {@code since}. Decay is not applied on a
 * timer; it is worked out from the elapsed time whenever the value is read, so unloaded chunks
 * decay too. Amounts below {@link #MIN_AMOUNT} are dropped, which removes the data from the chunk.
 */
public record FluxValue(double amount, long since) {
    public static final double MIN_AMOUNT = 1;

    public static final Codec<FluxValue> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("amount").forGetter(FluxValue::amount),
            Codec.LONG.fieldOf("since").forGetter(FluxValue::since)
    ).apply(i, FluxValue::new));

    /** The amount at game tick {@code now}, or 0 once it has decayed below {@link #MIN_AMOUNT}. */
    public double amountAt(long now, FluxSettings settings) {
        double decayed = settings.decay(amount, now - since);
        return decayed < MIN_AMOUNT ? 0 : decayed;
    }

    /** The value to store for {@code amount} at game tick {@code now}; empty when it is too small to keep. */
    public static Optional<FluxValue> of(double amount, long now) {
        return amount < MIN_AMOUNT ? Optional.empty() : Optional.of(new FluxValue(amount, now));
    }

    /** The amount stored in {@code value} at game tick {@code now}; 0 for no value. */
    public static double amountAt(Optional<FluxValue> value, long now, FluxSettings settings) {
        return value.map(v -> v.amountAt(now, settings)).orElse(0.0);
    }
}
