package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectCodecs;

/**
 * Server to client: the network of the pipe the player looks at through the loupe.
 *
 * @param carried    Essentia in transit in the whole network (a closed valve: what it keeps)
 * @param capacity   how much the network can hold in transit
 * @param pipes      pipes in the network
 * @param containers containers it touches
 */
public record PipeReadingPayload(BlockPos pos, AspectList carried, int capacity, int pipes, int containers) implements CustomPacketPayload {
    public static final Type<PipeReadingPayload> TYPE = new Type<>(Thaumory.id("pipe_reading"));
    public static final StreamCodec<ByteBuf, PipeReadingPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PipeReadingPayload::pos,
            ByteBufCodecs.fromCodec(AspectCodecs.aspectList(ThaumoryApi.aspects())), PipeReadingPayload::carried,
            ByteBufCodecs.VAR_INT, PipeReadingPayload::capacity,
            ByteBufCodecs.VAR_INT, PipeReadingPayload::pipes,
            ByteBufCodecs.VAR_INT, PipeReadingPayload::containers,
            PipeReadingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
