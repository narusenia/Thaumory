package one.nxeu.thaumory.knowledge;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.function.UnaryOperator;
import net.minecraft.server.level.ServerPlayer;
import one.nxeu.thaumory.network.KnowledgeSyncPayload;

/**
 * Reads and changes each player's knowledge, sending the whole of it to the player after every
 * change. Every change also goes past {@link Research}, which may complete chapters and find hints.
 */
public final class KnowledgeManager {
    /** What follows from knowledge: chapters and hints, and the book's view of them. */
    public interface Research {
        /** {@code knowledge} with whatever it now completes or finds added. */
        PlayerKnowledge advance(ServerPlayer player, PlayerKnowledge knowledge);

        /** Called after the player was sent their knowledge. */
        void synced(ServerPlayer player, PlayerKnowledge knowledge);
    }

    private final KnowledgeStorage storage;
    private Research research = new Research() {
        @Override
        public PlayerKnowledge advance(ServerPlayer player, PlayerKnowledge knowledge) {
            return knowledge;
        }

        @Override
        public void synced(ServerPlayer player, PlayerKnowledge knowledge) {}
    };

    public KnowledgeManager(KnowledgeStorage storage) {
        this.storage = storage;
    }

    public void registerEvents() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC);
        }
        PlayerEvent.PLAYER_JOIN.register(player -> {
            update(player, UnaryOperator.identity());
            sync(player);
        });
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> sync(player));
    }

    public void setResearch(Research research) {
        this.research = research;
    }

    public PlayerKnowledge get(ServerPlayer player) {
        return storage.get(player);
    }

    /** Applies {@code change}, then saves and syncs if anything changed. Returns the new knowledge. */
    public PlayerKnowledge update(ServerPlayer player, UnaryOperator<PlayerKnowledge> change) {
        PlayerKnowledge before = storage.get(player);
        PlayerKnowledge after = research.advance(player, change.apply(before));
        if (!after.equals(before)) {
            storage.set(player, after);
            sync(player);
        }
        return after;
    }

    private void sync(ServerPlayer player) {
        PlayerKnowledge knowledge = storage.get(player);
        NetworkManager.sendToPlayer(player, new KnowledgeSyncPayload(knowledge));
        research.synced(player, knowledge);
    }
}
