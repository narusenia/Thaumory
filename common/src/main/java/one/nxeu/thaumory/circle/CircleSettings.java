package one.nxeu.thaumory.circle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * {@code data/thaumory/thaumory/circle.json}: how often a Core rescans, the instability threshold
 * and the Flux going over it releases, the Flux an undefined combination releases, the radius each
 * ring count gives, what each rank above the first adds to the strength multiplier, how much of
 * each aspect a Core holds, and what each pattern (by block id) adds to instability and to the
 * strength, range and cost multipliers. Values not written are 0.
 *
 * <pre>{@code
 * {
 *   "scan_interval": 40,
 *   "instability_threshold": 3,
 *   "instability_flux": { "chance_per_point": 0.2, "flux_per_point": 2 },
 *   "undefined_flux": 5,
 *   "ring_radius": [4, 8, 16, 24, 32],
 *   "rank_strength": 0.25,
 *   "essentia_capacity": 64,
 *   "patterns": {
 *     "thaumory:amplifying_pattern": { "instability": 2, "strength": 0.5, "cost": 0.5 },
 *     "thaumory:extending_pattern": { "range": 0.5, "cost": 0.25 },
 *     "thaumory:economizing_pattern": { "cost": -0.25, "strength": -0.25 },
 *     "thaumory:stabilizing_pattern": { "instability": -3 }
 *   },
 *   "infusion": { "base_failure": 0.1, "failure_per_point": 0.2, "flux_ratio": 0.5, "max_level": 5, "item_essentia": 64 }
 * }
 * }</pre>
 */
public record CircleSettings(int scanInterval, int instabilityThreshold, InstabilityFlux instabilityFlux, double undefinedFlux,
        List<Integer> ringRadius, double rankStrength, int essentiaCapacity, Map<Identifier, PatternSettings> patterns, InfusionSettings infusion) {
    /** No multiplier goes below this, however many modifiers lower it. */
    public static final double MIN_MULTIPLIER = 0.25;

    public static final CircleSettings DEFAULT = new CircleSettings(40, 3, InstabilityFlux.DEFAULT, 5, List.of(4, 8, 16, 24, 32), 0.25, 64, Map.of(
            thaumory("amplifying_pattern"), new PatternSettings(2, 0.5, 0, 0.5),
            thaumory("extending_pattern"), new PatternSettings(0, 0, 0.5, 0.25),
            thaumory("economizing_pattern"), new PatternSettings(0, -0.25, 0, -0.25),
            thaumory("stabilizing_pattern"), new PatternSettings(-3, 0, 0, 0)), InfusionSettings.DEFAULT);

    /** Each point of instability over the threshold adds to the chance of Flux on a payment, and to its amount. */
    public record InstabilityFlux(double chancePerPoint, double fluxPerPoint) {
        public static final InstabilityFlux DEFAULT = new InstabilityFlux(0.2, 2);

        public static final Codec<InstabilityFlux> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("chance_per_point", DEFAULT.chancePerPoint)
                        .forGetter(InstabilityFlux::chancePerPoint),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("flux_per_point", DEFAULT.fluxPerPoint)
                        .forGetter(InstabilityFlux::fluxPerPoint)
        ).apply(i, InstabilityFlux::new));
    }

    public record PatternSettings(int instability, double strength, double range, double cost) {
        private static final PatternSettings NONE = new PatternSettings(0, 0, 0, 0);

        public static final Codec<PatternSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("instability", 0).forGetter(PatternSettings::instability),
                Codec.DOUBLE.optionalFieldOf("strength", 0.0).forGetter(PatternSettings::strength),
                Codec.DOUBLE.optionalFieldOf("range", 0.0).forGetter(PatternSettings::range),
                Codec.DOUBLE.optionalFieldOf("cost", 0.0).forGetter(PatternSettings::cost)
        ).apply(i, PatternSettings::new));
    }

    /** How the modifiers on a circle's nodes scale its strength, range and cost. */
    public record Multipliers(double strength, double range, double cost) {
        public static final Multipliers NONE = new Multipliers(1, 1, 1);
    }

    public static final Codec<CircleSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("scan_interval").forGetter(CircleSettings::scanInterval),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("instability_threshold").forGetter(CircleSettings::instabilityThreshold),
            InstabilityFlux.CODEC.optionalFieldOf("instability_flux", InstabilityFlux.DEFAULT).forGetter(CircleSettings::instabilityFlux),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("undefined_flux", DEFAULT.undefinedFlux)
                    .forGetter(CircleSettings::undefinedFlux),
            Codec.intRange(1, Integer.MAX_VALUE).listOf(1, CircleScan.MAX_RINGS)
                    .optionalFieldOf("ring_radius", DEFAULT.ringRadius).forGetter(CircleSettings::ringRadius),
            Codec.DOUBLE.optionalFieldOf("rank_strength", DEFAULT.rankStrength).forGetter(CircleSettings::rankStrength),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("essentia_capacity", DEFAULT.essentiaCapacity)
                    .forGetter(CircleSettings::essentiaCapacity),
            Codec.unboundedMap(Identifier.CODEC, PatternSettings.CODEC).optionalFieldOf("patterns", Map.of())
                    .forGetter(CircleSettings::patterns),
            InfusionSettings.CODEC.optionalFieldOf("infusion", InfusionSettings.DEFAULT).forGetter(CircleSettings::infusion)
    ).apply(i, CircleSettings::new));

    private PatternSettings pattern(Identifier id) {
        return patterns.getOrDefault(id, PatternSettings.NONE);
    }

    /** The sum of what the node patterns add, never below 0. */
    public int instability(List<CircleScan.Node> nodes) {
        int sum = 0;
        for (CircleScan.Node node : nodes) {
            sum += pattern(node.pattern()).instability();
        }
        return Math.max(0, sum);
    }

    /** The multipliers of a rank 1 Core. */
    public Multipliers multipliers(List<CircleScan.Node> nodes) {
        return multipliers(nodes, 1);
    }

    /**
     * Each multiplier is 1 plus what the node patterns add to it, never below {@link #MIN_MULTIPLIER}.
     * Each rank above the first adds {@code rank_strength} to the strength, the same way a pattern does.
     */
    public Multipliers multipliers(List<CircleScan.Node> nodes, int rank) {
        double strength = 1 + (rank - 1) * rankStrength;
        double range = 1;
        double cost = 1;
        for (CircleScan.Node node : nodes) {
            PatternSettings pattern = pattern(node.pattern());
            strength += pattern.strength();
            range += pattern.range();
            cost += pattern.cost();
        }
        return new Multipliers(Math.max(MIN_MULTIPLIER, strength), Math.max(MIN_MULTIPLIER, range), Math.max(MIN_MULTIPLIER, cost));
    }

    /**
     * The Flux one payment releases at this instability: none at or under the threshold, otherwise
     * {@code flux_per_point} for each point over it, with a chance of {@code chance_per_point} per
     * point (at most certain).
     *
     * @param roll a uniform random number in [0, 1)
     */
    public double instabilityFlux(int instability, double roll) {
        int excess = instability - instabilityThreshold;
        if (excess <= 0) {
            return 0;
        }
        double chance = Math.min(1, excess * instabilityFlux.chancePerPoint());
        return roll < chance ? excess * instabilityFlux.fluxPerPoint() : 0;
    }

    /** The radius a circle of {@code rings} rings reaches; 0 without rings. */
    public double radius(int rings, Multipliers multipliers) {
        return rings <= 0 ? 0 : ringRadius.get(Math.min(rings, ringRadius.size()) - 1) * multipliers.range();
    }

    private static Identifier thaumory(String path) {
        return Identifier.fromNamespaceAndPath("thaumory", path);
    }
}
