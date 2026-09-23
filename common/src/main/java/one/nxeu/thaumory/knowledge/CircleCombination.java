package one.nxeu.thaumory.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The Runes in a Core: two effect aspects (slots 1 and 2, in either order) and an optional
 * parameter aspect (slot 3). The effect aspects are stored sorted, so both orders are equal.
 */
public record CircleCombination(Identifier first, Identifier second, Optional<Identifier> parameter) {
    public static final Codec<CircleCombination> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("first").forGetter(CircleCombination::first),
            Identifier.CODEC.fieldOf("second").forGetter(CircleCombination::second),
            Identifier.CODEC.optionalFieldOf("parameter").forGetter(CircleCombination::parameter)
    ).apply(i, CircleCombination::new));

    public static final StreamCodec<ByteBuf, CircleCombination> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CircleCombination::first,
            Identifier.STREAM_CODEC, CircleCombination::second,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), CircleCombination::parameter,
            CircleCombination::new);

    public CircleCombination {
        if (first.compareTo(second) > 0) {
            Identifier swap = first;
            first = second;
            second = swap;
        }
    }

    @Override
    public String toString() {
        return first + " + " + second + parameter.map(p -> " / " + p).orElse("");
    }
}
