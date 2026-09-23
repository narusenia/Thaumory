package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** Server to client: everything the receiving player knows, replacing what the client had. */
public record KnowledgeSyncPayload(PlayerKnowledge knowledge) implements CustomPacketPayload {
    public static final Type<KnowledgeSyncPayload> TYPE = new Type<>(Thaumory.id("knowledge_sync"));
    public static final StreamCodec<ByteBuf, KnowledgeSyncPayload> STREAM_CODEC =
            PlayerKnowledge.STREAM_CODEC.map(KnowledgeSyncPayload::new, KnowledgeSyncPayload::knowledge);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
