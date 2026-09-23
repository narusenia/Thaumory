package one.nxeu.thaumory.fabric;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.knowledge.KnowledgeStorage;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** Keeps knowledge in a persistent player attachment that survives death. Synced by Thaumory's own packet. */
final class FabricKnowledgeStorage implements KnowledgeStorage {
    private final AttachmentType<PlayerKnowledge> type = AttachmentRegistry.create(Thaumory.id("knowledge"),
            builder -> builder.persistent(PlayerKnowledge.CODEC).copyOnDeath());

    @Override
    public PlayerKnowledge get(ServerPlayer player) {
        return player.getAttachedOrElse(type, PlayerKnowledge.EMPTY);
    }

    @Override
    public void set(ServerPlayer player, PlayerKnowledge knowledge) {
        player.setAttached(type, knowledge.equals(PlayerKnowledge.EMPTY) ? null : knowledge);
    }
}
