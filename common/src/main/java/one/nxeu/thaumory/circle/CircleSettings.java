package one.nxeu.thaumory.circle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * {@code data/thaumory/thaumory/circle.json}: how often a Core rescans, the instability threshold,
 * and what each pattern (by block id) adds to instability. Patterns not listed add nothing.
 *
 * <pre>{@code
 * {
 *   "scan_interval": 40,
 *   "instability_threshold": 3,
 *   "patterns": {
 *     "thaumory:amplifying_pattern": { "instability": 2 },
 *     "thaumory:stabilizing_pattern": { "instability": -3 }
 *   }
 * }
 * }</pre>
 */
public record CircleSettings(int scanInterval, int instabilityThreshold, Map<Identifier, PatternSettings> patterns) {
    public static final CircleSettings DEFAULT = new CircleSettings(40, 3, Map.of(
            Identifier.fromNamespaceAndPath("thaumory", "amplifying_pattern"), new PatternSettings(2),
            Identifier.fromNamespaceAndPath("thaumory", "stabilizing_pattern"), new PatternSettings(-3)));

    public record PatternSettings(int instability) {
        public static final Codec<PatternSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("instability", 0).forGetter(PatternSettings::instability)
        ).apply(i, PatternSettings::new));
    }

    public static final Codec<CircleSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("scan_interval").forGetter(CircleSettings::scanInterval),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("instability_threshold").forGetter(CircleSettings::instabilityThreshold),
            Codec.unboundedMap(Identifier.CODEC, PatternSettings.CODEC).optionalFieldOf("patterns", Map.of())
                    .forGetter(CircleSettings::patterns)
    ).apply(i, CircleSettings::new));

    /** The sum of what the node patterns add, never below 0. */
    public int instability(List<CircleScan.Node> nodes) {
        int sum = 0;
        for (CircleScan.Node node : nodes) {
            PatternSettings pattern = patterns.get(node.pattern());
            sum += pattern == null ? 0 : pattern.instability();
        }
        return Math.max(0, sum);
    }
}
