package one.nxeu.thaumory.block.stone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.knowledge.CircleCombination;

/**
 * The circle burnt into a circle stone (requirements §10.3): its runes, and the effect they made
 * when it was burnt, which names the stone. The stone looks its combination up again whenever it
 * works, so a datapack that changes the combination changes the stone too.
 */
public record BurntCircle(CircleCombination combination, Identifier effect) {
    public static final Codec<BurntCircle> CODEC = RecordCodecBuilder.create(i -> i.group(
            CircleCombination.CODEC.fieldOf("combination").forGetter(BurntCircle::combination),
            Identifier.CODEC.fieldOf("effect").forGetter(BurntCircle::effect)
    ).apply(i, BurntCircle::new));

    public static final StreamCodec<ByteBuf, BurntCircle> STREAM_CODEC = StreamCodec.composite(
            CircleCombination.STREAM_CODEC, BurntCircle::combination,
            Identifier.STREAM_CODEC, BurntCircle::effect,
            BurntCircle::new);
}
