package one.nxeu.thaumory.wand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;

/** What a wand is made of (requirements §7.1): its caps' item and its core's item. */
public record WandBuild(Identifier cap, Identifier core) {
    /** A wand with no parts recorded: gold caps on a wooden core, as wands were before they had parts. */
    public static final WandBuild DEFAULT = new WandBuild(Thaumory.id("gold_wand_cap"), Identifier.withDefaultNamespace("stick"));

    public static final Codec<WandBuild> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("cap").forGetter(WandBuild::cap),
            Identifier.CODEC.fieldOf("core").forGetter(WandBuild::core)
    ).apply(i, WandBuild::new));

    public static final StreamCodec<ByteBuf, WandBuild> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, WandBuild::cap,
            Identifier.STREAM_CODEC, WandBuild::core,
            WandBuild::new);

    public WandBuild withCap(Identifier updated) {
        return new WandBuild(updated, core);
    }

    public WandBuild withCore(Identifier updated) {
        return new WandBuild(cap, updated);
    }
}
