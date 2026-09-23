package one.nxeu.thaumory.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.PacketTransformer;
import dev.architectury.networking.transformers.SplitPacketTransformer;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.data.ItemAspects;

/** Sends every item's aspects to players when they join and whenever the server recomputes them. */
public final class AspectSync {
    private static volatile AspectSyncPayload latest;

    private AspectSync() {}

    /** Large modpacks can exceed one packet, so payloads are split. Each side needs its own instance. */
    static List<PacketTransformer> transformers() {
        return List.of(new SplitPacketTransformer());
    }

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(AspectSyncPayload.TYPE, AspectSyncPayload.STREAM_CODEC, transformers());
        }
        PlayerEvent.PLAYER_JOIN.register(player -> NetworkManager.sendToPlayer(player, payload()));
    }

    /** Takes a fresh snapshot and sends it to everyone online. */
    public static void sendToAll(MinecraftServer server) {
        latest = snapshot();
        NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), latest);
    }

    private static AspectSyncPayload payload() {
        AspectSyncPayload payload = latest;
        if (payload == null) {
            payload = latest = snapshot();
        }
        return payload;
    }

    private static AspectSyncPayload snapshot() {
        Map<Identifier, AspectList> items = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            AspectList aspects = ItemAspects.get(item);
            if (!aspects.isEmpty()) {
                items.put(BuiltInRegistries.ITEM.getKey(item), aspects);
            }
        }
        return new AspectSyncPayload(items);
    }
}
