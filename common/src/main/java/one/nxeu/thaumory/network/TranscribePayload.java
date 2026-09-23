package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.knowledge.Transcript;

/** Client to server: the player pressed "transcribe" in the book on this piece of knowledge. */
public record TranscribePayload(Transcript transcript) implements CustomPacketPayload {
    public static final Type<TranscribePayload> TYPE = new Type<>(Thaumory.id("transcribe"));
    public static final StreamCodec<ByteBuf, TranscribePayload> STREAM_CODEC =
            Transcript.STREAM_CODEC.map(TranscribePayload::new, TranscribePayload::transcript);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
