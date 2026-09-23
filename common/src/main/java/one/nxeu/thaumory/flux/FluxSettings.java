package one.nxeu.thaumory.flux;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import one.nxeu.thaumory.api.flux.FluxStage;

/**
 * {@code data/thaumory/thaumory/flux.json}: how fast Flux decays and where each stage starts. A
 * higher datapack replaces the whole file.
 *
 * <pre>{@code
 * {
 *   "decay": { "fraction": 0.1, "interval": 72000 },
 *   "stages": { "stagnation": 50, "erosion": 150, "manifestation": 300, "overload": 500 }
 * }
 * }</pre>
 *
 * The {@code fraction} of Flux disappears every {@code interval} game ticks, continuously.
 */
public record FluxSettings(Decay decay, Stages stages) {
    public static final FluxSettings DEFAULT = new FluxSettings(new Decay(0.1, 72_000), new Stages(50, 150, 300, 500));

    public static final Codec<FluxSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Decay.CODEC.fieldOf("decay").forGetter(FluxSettings::decay),
            Stages.CODEC.fieldOf("stages").forGetter(FluxSettings::stages)
    ).apply(i, FluxSettings::new));

    /** What is left of {@code amount} after {@code elapsedTicks} of natural decay. */
    public double decay(double amount, long elapsedTicks) {
        if (elapsedTicks <= 0) {
            return amount;
        }
        return amount * Math.pow(1 - decay.fraction(), (double) elapsedTicks / decay.interval());
    }

    /** The stage an amount has reached. Only whole units count. */
    public FluxStage stage(double amount) {
        long whole = (long) Math.floor(amount);
        if (whole >= stages.overload()) {
            return FluxStage.OVERLOAD;
        } else if (whole >= stages.manifestation()) {
            return FluxStage.MANIFESTATION;
        } else if (whole >= stages.erosion()) {
            return FluxStage.EROSION;
        } else if (whole >= stages.stagnation()) {
            return FluxStage.STAGNATION;
        }
        return FluxStage.NONE;
    }

    public record Decay(double fraction, long interval) {
        public static final Codec<Decay> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, 1).fieldOf("fraction").forGetter(Decay::fraction),
                Codec.LONG.validate(interval -> interval > 0
                        ? DataResult.success(interval)
                        : DataResult.error(() -> "Decay interval must be positive: " + interval))
                        .fieldOf("interval").forGetter(Decay::interval)
        ).apply(i, Decay::new));
    }

    /** The amount each stage starts at. Each must be higher than the one before. */
    public record Stages(int stagnation, int erosion, int manifestation, int overload) {
        public static final Codec<Stages> CODEC = RecordCodecBuilder.<Stages>create(i -> i.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("stagnation").forGetter(Stages::stagnation),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("erosion").forGetter(Stages::erosion),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("manifestation").forGetter(Stages::manifestation),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("overload").forGetter(Stages::overload)
        ).apply(i, Stages::new)).validate(Stages::checkAscending);

        private static DataResult<Stages> checkAscending(Stages stages) {
            return stages.stagnation < stages.erosion && stages.erosion < stages.manifestation && stages.manifestation < stages.overload
                    ? DataResult.success(stages)
                    : DataResult.error(() -> "Flux stages must rise from stagnation to overload: " + stages);
        }
    }
}
