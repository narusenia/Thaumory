package one.nxeu.thaumory.rune;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** {@code data/thaumory/thaumory/rune.json}: {@code {"cost": 16}}, the Essentia one rune takes. */
public record RuneSettings(int cost) {
    public static final RuneSettings DEFAULT = new RuneSettings(16);

    public static final Codec<RuneSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("cost").forGetter(RuneSettings::cost)
    ).apply(i, RuneSettings::new));
}
