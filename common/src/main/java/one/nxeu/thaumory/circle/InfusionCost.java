package one.nxeu.thaumory.circle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * What infusing a combination takes before the cost multiplier: {@code runes} of each effect
 * rune's aspect and {@code parameter} of slot 3's. A combination file's {@code "infusion_cost"}.
 */
public record InfusionCost(int runes, int parameter) {
    public static final InfusionCost DEFAULT = new InfusionCost(32, 4);

    public static final Codec<InfusionCost> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("runes", DEFAULT.runes).forGetter(InfusionCost::runes),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("parameter", DEFAULT.parameter).forGetter(InfusionCost::parameter)
    ).apply(i, InfusionCost::new));
}
