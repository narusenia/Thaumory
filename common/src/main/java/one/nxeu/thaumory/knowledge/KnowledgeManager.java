package one.nxeu.thaumory.knowledge;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;
import one.nxeu.thaumory.network.KnowledgeSyncPayload;

/** Reads and changes each player's knowledge, sending the whole of it to the player after every change. */
public final class KnowledgeManager {
    private final KnowledgeStorage storage;

    public KnowledgeManager(KnowledgeStorage storage) {
        this.storage = storage;
    }

    public void registerEvents() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC);
        }
        PlayerEvent.PLAYER_JOIN.register(this::sync);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> sync(player));
    }

    public PlayerKnowledge get(ServerPlayer player) {
        return storage.get(player);
    }

    /** Applies {@code change}, then saves and syncs if anything changed. Returns the new knowledge. */
    public PlayerKnowledge update(ServerPlayer player, UnaryOperator<PlayerKnowledge> change) {
        PlayerKnowledge before = storage.get(player);
        PlayerKnowledge after = change.apply(before);
        if (!after.equals(before)) {
            storage.set(player, after);
            sync(player);
        }
        return after;
    }

    private void sync(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, new KnowledgeSyncPayload(storage.get(player)));
    }
}
