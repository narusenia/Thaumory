package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;

/** Client to server: the player pressed the key to use an infused item's active effect. */
public record UseInfusionPayload() implements CustomPacketPayload {
    public static final UseInfusionPayload INSTANCE = new UseInfusionPayload();
    public static final Type<UseInfusionPayload> TYPE = new Type<>(Thaumory.id("use_infusion"));
    public static final StreamCodec<ByteBuf, UseInfusionPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
