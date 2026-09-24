package one.nxeu.thaumory.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
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
 * @param useCost what one use of an active effect takes from the Essentia stored in the item, by
 *                aspect id, as it was when burnt in. Empty for a passive effect.
 */
public record Infusion(Identifier effect, int level, Optional<Identifier> parameter, int capacity, Map<Identifier, Integer> useCost) {
    public static final Codec<Infusion> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("effect").forGetter(Infusion::effect),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("level").forGetter(Infusion::level),
            Identifier.CODEC.optionalFieldOf("parameter").forGetter(Infusion::parameter),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("capacity", 1).forGetter(Infusion::capacity),
            Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, Integer.MAX_VALUE)).optionalFieldOf("use_cost", Map.of())
                    .forGetter(Infusion::useCost)
    ).apply(i, Infusion::new));

    public static final StreamCodec<ByteBuf, Infusion> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Infusion::effect,
            ByteBufCodecs.VAR_INT, Infusion::level,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), Infusion::parameter,
            ByteBufCodecs.VAR_INT, Infusion::capacity,
            ByteBufCodecs.<ByteBuf, Identifier, Integer, Map<Identifier, Integer>>map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            Infusion::useCost,
            Infusion::new);

    public Infusion {
        useCost = Map.copyOf(useCost);
    }

    /** A passive effect: nothing to pay. */
    public Infusion(Identifier effect, int level, Optional<Identifier> parameter, int capacity) {
        this(effect, level, parameter, capacity, Map.of());
    }

    /** Whether this effect is used with the key and pays from the item's Essentia. */
    public boolean active() {
        return !useCost.isEmpty();
    }
}
