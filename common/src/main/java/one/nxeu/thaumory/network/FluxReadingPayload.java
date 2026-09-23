package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.flux.FluxStage;

/** Server to client: the Flux in the chunk the player stands in, for the loupe's reading. */
public record FluxReadingPayload(double amount, FluxStage stage) implements CustomPacketPayload {
    public static final Type<FluxReadingPayload> TYPE = new Type<>(Thaumory.id("flux_reading"));
    public static final StreamCodec<ByteBuf, FluxReadingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, FluxReadingPayload::amount,
            ByteBufCodecs.idMapper(i -> FluxStage.values()[i], FluxStage::ordinal), FluxReadingPayload::stage,
            FluxReadingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
