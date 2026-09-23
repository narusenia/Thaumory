package one.nxeu.thaumory.jar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** {@code data/thaumory/thaumory/jar.json}: {@code {"capacity": 64}}. */
public record JarSettings(int capacity) {
    public static final JarSettings DEFAULT = new JarSettings(64);

    public static final Codec<JarSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("capacity").forGetter(JarSettings::capacity)
    ).apply(i, JarSettings::new));
}
