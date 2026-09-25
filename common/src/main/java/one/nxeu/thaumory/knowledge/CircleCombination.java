package one.nxeu.thaumory.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The Runes in a Core: two effect aspects (slots 1 and 2, in either order), an optional
 * parameter aspect (slot 3) and an optional slot 4 aspect. The effect aspects are stored sorted, so
 * both orders are equal.
 */
public record CircleCombination(Identifier first, Identifier second, Optional<Identifier> parameter, Optional<Identifier> slot4) {
    public static final Codec<CircleCombination> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("first").forGetter(CircleCombination::first),
            Identifier.CODEC.fieldOf("second").forGetter(CircleCombination::second),
            Identifier.CODEC.optionalFieldOf("parameter").forGetter(CircleCombination::parameter),
            Identifier.CODEC.optionalFieldOf("slot4").forGetter(CircleCombination::slot4)
    ).apply(i, CircleCombination::new));

    public static final StreamCodec<ByteBuf, CircleCombination> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CircleCombination::first,
            Identifier.STREAM_CODEC, CircleCombination::second,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), CircleCombination::parameter,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), CircleCombination::slot4,
            CircleCombination::new);

    public CircleCombination {
        if (first.compareTo(second) > 0) {
            Identifier swap = first;
            first = second;
            second = swap;
        }
    }

    /** A combination with slot 4 empty. */
    public CircleCombination(Identifier first, Identifier second, Optional<Identifier> parameter) {
        this(first, second, parameter, Optional.empty());
    }

    @Override
    public String toString() {
        return first + " + " + second + parameter.map(p -> " / " + p).orElse("") + slot4.map(s -> " / " + s).orElse("");
    }
}
