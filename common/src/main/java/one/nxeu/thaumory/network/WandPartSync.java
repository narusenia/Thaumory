package one.nxeu.thaumory.network;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import one.nxeu.thaumory.wand.WandFoci;
import one.nxeu.thaumory.wand.WandParts;

/** Sends the wand parts and foci to players when they join and after each datapack reload, for wand tooltips. */
public final class WandPartSync {
    private static volatile MinecraftServer server;

    private WandPartSync() {}

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(WandPartSyncPayload.TYPE, WandPartSyncPayload.STREAM_CODEC);
        }
        LifecycleEvent.SERVER_STARTING.register(s -> server = s);
        LifecycleEvent.SERVER_STOPPED.register(s -> server = null);
        PlayerEvent.PLAYER_JOIN.register(player -> NetworkManager.sendToPlayer(player, payload()));
        LifecycleEvent.TAGS_UPDATED.register((registries, client) -> {
            MinecraftServer current = server;
            if (!client && current != null) {
                current.execute(() -> NetworkManager.sendToPlayers(current.getPlayerList().getPlayers(), payload()));
            }
        });
    }

    private static WandPartSyncPayload payload() {
        return new WandPartSyncPayload(List.copyOf(WandParts.snapshot().values()), List.copyOf(WandFoci.snapshot().values()));
    }
}
