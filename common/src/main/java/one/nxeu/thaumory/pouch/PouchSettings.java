package one.nxeu.thaumory.pouch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * {@code data/thaumory/thaumory/pouch.json}: {@code {"interval": 20, "per_transfer": 4}}. Every
 * {@code interval} ticks an Essentia pouch gives each item up to {@code per_transfer} of each aspect.
 */
public record PouchSettings(int interval, int perTransfer) {
    public static final PouchSettings DEFAULT = new PouchSettings(20, 4);

    public static final Codec<PouchSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("interval", DEFAULT.interval()).forGetter(PouchSettings::interval),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("per_transfer", DEFAULT.perTransfer()).forGetter(PouchSettings::perTransfer)
    ).apply(i, PouchSettings::new));
}
