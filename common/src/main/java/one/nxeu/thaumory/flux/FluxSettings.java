package one.nxeu.thaumory.flux;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import one.nxeu.thaumory.api.flux.FluxStage;

/**
 * {@code data/thaumory/thaumory/flux.json}: how fast Flux decays, where each stage starts, and how
 * strongly each stage acts on the world ({@link Effects}, optional). A higher datapack replaces the
 * whole file.
 *
 * <pre>{@code
 * {
 *   "decay": { "fraction": 0.1, "interval": 72000 },
 *   "stages": { "stagnation": 50, "erosion": 150, "manifestation": 300, "overload": 500 },
 *   "effects": { "particles": [2, 4, 6, 10], "pollution_attempts": [1, 2, 3], "misfire_chance": 0.25, ... }
 * }
 * }</pre>
 *
 * The {@code fraction} of Flux disappears every {@code interval} game ticks, continuously.
 */
public record FluxSettings(Decay decay, Stages stages, Effects effects) {
    public static final FluxSettings DEFAULT = new FluxSettings(new Decay(0.1, 72_000), new Stages(50, 150, 300, 500), Effects.DEFAULT);

    public static final Codec<FluxSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Decay.CODEC.fieldOf("decay").forGetter(FluxSettings::decay),
            Stages.CODEC.fieldOf("stages").forGetter(FluxSettings::stages),
            Effects.CODEC.optionalFieldOf("effects", Effects.DEFAULT).forGetter(FluxSettings::effects)
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

    /**
     * How each stage acts on the world near players: particles from stagnation on, polluted blocks
     * from erosion on, Void Remnants and extra circle instability from manifestation on, and
     * misfiring circles at overload. Stage lists run in stage order from where the effect starts.
     */
    public record Effects(List<Integer> particles, int pollutionInterval, List<Integer> pollutionAttempts, int spawnInterval,
            double spawnChance, int spawnCap, int manifestationInstability, double misfireChance, double explosionRadius,
            float explosionDamage) {
        public static final Effects DEFAULT = new Effects(List.of(2, 4, 6, 10), 200, List.of(1, 2, 3), 600, 0.5, 3, 3, 0.25, 4, 4);

        private static final Codec<Integer> COUNT = Codec.intRange(0, Integer.MAX_VALUE);
        private static final Codec<Integer> INTERVAL = Codec.intRange(1, Integer.MAX_VALUE);
        private static final Codec<Double> CHANCE = Codec.doubleRange(0, 1);

        public static final Codec<Effects> CODEC = RecordCodecBuilder.create(i -> i.group(
                COUNT.listOf(4, 4).optionalFieldOf("particles", DEFAULT.particles).forGetter(Effects::particles),
                INTERVAL.optionalFieldOf("pollution_interval", DEFAULT.pollutionInterval).forGetter(Effects::pollutionInterval),
                COUNT.listOf(3, 3).optionalFieldOf("pollution_attempts", DEFAULT.pollutionAttempts).forGetter(Effects::pollutionAttempts),
                INTERVAL.optionalFieldOf("spawn_interval", DEFAULT.spawnInterval).forGetter(Effects::spawnInterval),
                CHANCE.optionalFieldOf("spawn_chance", DEFAULT.spawnChance).forGetter(Effects::spawnChance),
                COUNT.optionalFieldOf("spawn_cap", DEFAULT.spawnCap).forGetter(Effects::spawnCap),
                COUNT.optionalFieldOf("manifestation_instability", DEFAULT.manifestationInstability).forGetter(Effects::manifestationInstability),
                CHANCE.optionalFieldOf("misfire_chance", DEFAULT.misfireChance).forGetter(Effects::misfireChance),
                Codec.doubleRange(0, 16).optionalFieldOf("explosion_radius", DEFAULT.explosionRadius).forGetter(Effects::explosionRadius),
                Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("explosion_damage", DEFAULT.explosionDamage).forGetter(Effects::explosionDamage)
        ).apply(i, Effects::new));

        /** Particles per chunk each time, 0 below stagnation. */
        public int particles(FluxStage stage) {
            return stage.compareTo(FluxStage.STAGNATION) < 0 ? 0 : particles.get(stage.ordinal() - FluxStage.STAGNATION.ordinal());
        }

        /** Surface blocks tried per chunk each time, 0 below erosion. */
        public int pollutionAttempts(FluxStage stage) {
            return stage.compareTo(FluxStage.EROSION) < 0 ? 0 : pollutionAttempts.get(stage.ordinal() - FluxStage.EROSION.ordinal());
        }

        /** What a Core's chunk adds to its circle's instability. */
        public int extraInstability(FluxStage stage) {
            return stage.compareTo(FluxStage.MANIFESTATION) >= 0 ? manifestationInstability : 0;
        }

        /** The chance that one payment of a circle misfires. */
        public double misfireChance(FluxStage stage) {
            return stage == FluxStage.OVERLOAD ? misfireChance : 0;
        }

        public boolean spawnsRemnants(FluxStage stage) {
            return stage.compareTo(FluxStage.MANIFESTATION) >= 0;
        }

        public boolean stopsCrops(FluxStage stage) {
            return stage.compareTo(FluxStage.EROSION) >= 0;
        }
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
