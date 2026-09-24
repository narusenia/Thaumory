package one.nxeu.thaumory.circle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * {@code "infusion"} in {@code circle.json}: {@code {"base_failure": 0.1, "failure_per_point": 0.2,
 * "flux_ratio": 0.5, "max_level": 5, "item_essentia": 64}}.
 *
 * @param baseFailure     the chance an infusion fails at or under the instability threshold
 * @param failurePerPoint what each point of instability over the threshold adds to it
 * @param fluxRatio       the share of a failed infusion's Essentia that becomes Flux
 * @param maxLevel        the highest level an infusion reaches, however strong the circle
 * @param itemEssentia    how much of each aspect an item with an active effect stores
 */
public record InfusionSettings(double baseFailure, double failurePerPoint, double fluxRatio, int maxLevel, int itemEssentia) {
    public static final InfusionSettings DEFAULT = new InfusionSettings(0.1, 0.2, 0.5, 5, 64);

    public static final Codec<InfusionSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 1).optionalFieldOf("base_failure", DEFAULT.baseFailure).forGetter(InfusionSettings::baseFailure),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("failure_per_point", DEFAULT.failurePerPoint)
                    .forGetter(InfusionSettings::failurePerPoint),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("flux_ratio", DEFAULT.fluxRatio).forGetter(InfusionSettings::fluxRatio),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_level", DEFAULT.maxLevel).forGetter(InfusionSettings::maxLevel),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("item_essentia", DEFAULT.itemEssentia).forGetter(InfusionSettings::itemEssentia)
    ).apply(i, InfusionSettings::new));
}
