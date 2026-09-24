package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;

/** Server to client: every item's infusion capacity, replacing what the client had. Items without one are left out. */
public record CapacitySyncPayload(Map<Identifier, Integer> items) implements CustomPacketPayload {
    public static final Type<CapacitySyncPayload> TYPE = new Type<>(Thaumory.id("capacity_sync"));
    public static final StreamCodec<ByteBuf, CapacitySyncPayload> STREAM_CODEC = ByteBufCodecs
            .map(HashMap<Identifier, Integer>::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT)
            .map(CapacitySyncPayload::new, payload -> new HashMap<>(payload.items()));

    public CapacitySyncPayload {
        items = Map.copyOf(items);
    }

    @Override
    public Type<CapacitySyncPayload> type() {
        return TYPE;
    }
}
