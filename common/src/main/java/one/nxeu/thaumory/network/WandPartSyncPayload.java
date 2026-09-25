package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.wand.WandPart;

/** Server to client: every wand part the datapacks define, replacing what the client had, for tooltips. */
public record WandPartSyncPayload(List<WandPart> parts) implements CustomPacketPayload {
    public static final Type<WandPartSyncPayload> TYPE = new Type<>(Thaumory.id("wand_part_sync"));
    public static final StreamCodec<ByteBuf, WandPartSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(WandPart.CODEC.listOf()), WandPartSyncPayload::parts,
            WandPartSyncPayload::new);

    public WandPartSyncPayload {
        parts = List.copyOf(parts);
    }

    @Override
    public Type<WandPartSyncPayload> type() {
        return TYPE;
    }
}
