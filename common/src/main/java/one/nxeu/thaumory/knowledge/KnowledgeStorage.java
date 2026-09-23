package one.nxeu.thaumory.knowledge;

import net.minecraft.server.level.ServerPlayer;

/** Saves {@link PlayerKnowledge} with a player, keeping it through death. Each loader implements it. */
public interface KnowledgeStorage {
    PlayerKnowledge get(ServerPlayer player);

    void set(ServerPlayer player, PlayerKnowledge knowledge);
}
