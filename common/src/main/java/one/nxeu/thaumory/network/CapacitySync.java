package one.nxeu.thaumory.network;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.MinecraftServer;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.infusion.InfusionCapacities;

/** Sends every item's infusion capacity (and what an item stores for an active effect) to players when they join and after each datapack reload, for tooltips. */
public final class CapacitySync {
    private static volatile MinecraftServer server;

    private CapacitySync() {}

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(CapacitySyncPayload.TYPE, CapacitySyncPayload.STREAM_CODEC);
        }
        LifecycleEvent.SERVER_STARTING.register(s -> server = s);
        LifecycleEvent.SERVER_STOPPED.register(s -> server = null);
        PlayerEvent.PLAYER_JOIN.register(player -> NetworkManager.sendToPlayer(player, payload()));
        LifecycleEvent.TAGS_UPDATED.register((registries, client) -> {
            if (client) {
                return;
            }
            // Capacities follow item tags, so what was looked up before the tags bound is stale.
            InfusionCapacities.invalidate();
            MinecraftServer current = server;
            if (current != null) {
                current.execute(() -> NetworkManager.sendToPlayers(current.getPlayerList().getPlayers(), payload()));
            }
        });
    }

    private static CapacitySyncPayload payload() {
        return new CapacitySyncPayload(InfusionCapacities.snapshot(), CircleCoreBlockEntity.settings().infusion().itemEssentia());
    }
}
