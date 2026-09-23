package one.nxeu.thaumory.network;

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;

/**
 * Server to client: the aspects of every item that has any, replacing what the client had.
 * Aspects the client does not know (a mod missing on one side) are dropped while decoding.
 */
public record AspectSyncPayload(Map<Identifier, AspectList> items) implements CustomPacketPayload {
    public static final Type<AspectSyncPayload> TYPE = new Type<>(Thaumory.id("aspect_sync"));
    public static final StreamCodec<ByteBuf, AspectSyncPayload> STREAM_CODEC = codec(ThaumoryApi.aspects());

    public AspectSyncPayload {
        items = Map.copyOf(items);
    }

    static StreamCodec<ByteBuf, AspectSyncPayload> codec(AspectRegistry registry) {
        return StreamCodec.of((buf, payload) -> encode(buf, payload), buf -> decode(buf, registry));
    }

    private static void encode(ByteBuf buf, AspectSyncPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buf, payload.items.size());
        payload.items.forEach((item, aspects) -> {
            Identifier.STREAM_CODEC.encode(buf, item);
            ByteBufCodecs.VAR_INT.encode(buf, aspects.size());
            for (AspectStack stack : aspects.stacks()) {
                Identifier.STREAM_CODEC.encode(buf, stack.aspect().id());
                ByteBufCodecs.VAR_INT.encode(buf, stack.amount());
            }
        });
    }

    private static AspectSyncPayload decode(ByteBuf buf, AspectRegistry registry) {
        int itemCount = ByteBufCodecs.VAR_INT.decode(buf);
        Map<Identifier, AspectList> items = new LinkedHashMap<>();
        for (int i = 0; i < itemCount; i++) {
            Identifier item = Identifier.STREAM_CODEC.decode(buf);
            int aspectCount = ByteBufCodecs.VAR_INT.decode(buf);
            AspectList.Builder aspects = AspectList.builder();
            for (int j = 0; j < aspectCount; j++) {
                Identifier aspectId = Identifier.STREAM_CODEC.decode(buf);
                int amount = ByteBufCodecs.VAR_INT.decode(buf);
                registry.get(aspectId).ifPresent((Aspect aspect) -> aspects.add(aspect, amount));
            }
            AspectList list = aspects.build();
            if (!list.isEmpty()) {
                items.put(item, list);
            }
        }
        return new AspectSyncPayload(items);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
