package one.nxeu.thaumory.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * One circle effect burnt into an item (requirements §10.1). Kept by id, so an effect whose addon
 * was removed comes back with it.
 *
 * @param parameter slot 3's aspect when infused, such as a teleport's channel
 * @param capacity the item's capacity it takes, as it was when burnt in (requirements §10.2)
 */
public record Infusion(Identifier effect, int level, Optional<Identifier> parameter, int capacity) {
    public static final Codec<Infusion> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("effect").forGetter(Infusion::effect),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("level").forGetter(Infusion::level),
            Identifier.CODEC.optionalFieldOf("parameter").forGetter(Infusion::parameter),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("capacity", 1).forGetter(Infusion::capacity)
    ).apply(i, Infusion::new));

    public static final StreamCodec<ByteBuf, Infusion> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Infusion::effect,
            ByteBufCodecs.VAR_INT, Infusion::level,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), Infusion::parameter,
            ByteBufCodecs.VAR_INT, Infusion::capacity,
            Infusion::new);
}
