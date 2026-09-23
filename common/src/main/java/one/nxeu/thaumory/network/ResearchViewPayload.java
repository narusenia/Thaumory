package one.nxeu.thaumory.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.research.ResearchView;

/** Server to client: the chapters the receiving player can see, replacing what the client had. */
public record ResearchViewPayload(ResearchView view) implements CustomPacketPayload {
    public static final Type<ResearchViewPayload> TYPE = new Type<>(Thaumory.id("research_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchViewPayload> STREAM_CODEC =
            ResearchView.STREAM_CODEC.map(ResearchViewPayload::new, ResearchViewPayload::view);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
