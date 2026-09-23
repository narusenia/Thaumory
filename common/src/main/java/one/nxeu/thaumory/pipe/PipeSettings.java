package one.nxeu.thaumory.pipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * {@code data/thaumory/thaumory/pipe.json}: {@code {"interval": 10, "rate": 4, "buffer_per_pipe": 4}}.
 *
 * @param interval      ticks between two steps of a network
 * @param rate          Essentia one receiver takes per step
 * @param bufferPerPipe Essentia each pipe adds to what its network can hold in transit
 */
public record PipeSettings(int interval, int rate, int bufferPerPipe) {
    public static final PipeSettings DEFAULT = new PipeSettings(10, 4, 4);

    public static final Codec<PipeSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("interval").forGetter(PipeSettings::interval),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("rate").forGetter(PipeSettings::rate),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("buffer_per_pipe").forGetter(PipeSettings::bufferPerPipe)
    ).apply(i, PipeSettings::new));
}
