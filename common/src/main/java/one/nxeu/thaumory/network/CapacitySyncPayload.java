package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;

/**
 * Server to client: every item's infusion capacity, replacing what the client had (items without one
 * are left out), and how much of each aspect an item with an active effect stores.
 */
public record CapacitySyncPayload(Map<Identifier, Integer> items, int itemEssentia) implements CustomPacketPayload {
    public static final Type<CapacitySyncPayload> TYPE = new Type<>(Thaumory.id("capacity_sync"));
    public static final StreamCodec<ByteBuf, CapacitySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap<Identifier, Integer>::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT),
            payload -> new HashMap<>(payload.items()),
            ByteBufCodecs.VAR_INT, CapacitySyncPayload::itemEssentia,
            CapacitySyncPayload::new);

    public CapacitySyncPayload {
        items = Map.copyOf(items);
    }

    @Override
    public Type<CapacitySyncPayload> type() {
        return TYPE;
    }
}
