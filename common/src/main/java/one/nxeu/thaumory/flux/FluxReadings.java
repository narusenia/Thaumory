package one.nxeu.thaumory.flux;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.item.ArcaneLoupeItem;
import one.nxeu.thaumory.network.FluxReadingPayload;

/** Once a second, tells each player holding the Arcane Loupe how much Flux their chunk holds. */
public final class FluxReadings {
    private static final int INTERVAL = 20;

    private FluxReadings() {}

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(FluxReadingPayload.TYPE, FluxReadingPayload.STREAM_CODEC);
        }
        TickEvent.SERVER_POST.register(server -> {
            if (server.getTickCount() % INTERVAL != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ArcaneLoupeItem.isHeldBy(player)) {
                    ChunkPos chunk = player.chunkPosition();
                    NetworkManager.sendToPlayer(player, new FluxReadingPayload(
                            ThaumoryApi.flux().get(player.level(), chunk), ThaumoryApi.flux().stage(player.level(), chunk)));
                }
            }
        });
    }
}
