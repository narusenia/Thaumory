package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;

/**
 * Client to server: the player chose in the focus menu. {@code slot} is the inventory slot of the focus
 * to put on the wand in their main hand, or {@link #DETACH} to take its focus off.
 */
public record SelectFocusPayload(int slot) implements CustomPacketPayload {
    public static final int DETACH = -1;
    public static final Type<SelectFocusPayload> TYPE = new Type<>(Thaumory.id("select_focus"));
    public static final StreamCodec<ByteBuf, SelectFocusPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectFocusPayload::slot,
            SelectFocusPayload::new);

    @Override
    public Type<SelectFocusPayload> type() {
        return TYPE;
    }
}
