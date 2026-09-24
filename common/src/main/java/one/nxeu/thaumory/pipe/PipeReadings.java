package one.nxeu.thaumory.pipe;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import one.nxeu.thaumory.block.pipe.EssentiaPipeBlock;
import one.nxeu.thaumory.item.ArcaneLoupeItem;
import one.nxeu.thaumory.network.PipeReadingPayload;

/**
 * Four times a second, tells each player looking at a pipe through the loupe what its network
 * carries (requirements §7.2). Networks live on the server only, so the client cannot see this.
 */
public final class PipeReadings {
    private static final int INTERVAL = 5;

    private PipeReadings() {}

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(PipeReadingPayload.TYPE, PipeReadingPayload.STREAM_CODEC);
        }
        TickEvent.SERVER_POST.register(server -> {
            if (server.getTickCount() % INTERVAL != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (ArcaneLoupeItem.isHeldBy(player)) {
                    send(player);
                }
            }
        });
    }

    private static void send(ServerPlayer player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0f, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK
                || !(player.level().getBlockState(block.getBlockPos()).getBlock() instanceof EssentiaPipeBlock)) {
            return;
        }
        PipeNetworks.of(player.level()).reading(block.getBlockPos()).ifPresent(reading -> NetworkManager.sendToPlayer(player, reading));
    }
}
