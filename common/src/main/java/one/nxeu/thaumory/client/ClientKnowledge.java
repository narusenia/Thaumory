package one.nxeu.thaumory.client;

import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** The local player's knowledge as last received from the server. */
public final class ClientKnowledge {
    private static volatile PlayerKnowledge knowledge = PlayerKnowledge.EMPTY;

    private ClientKnowledge() {}

    public static PlayerKnowledge get() {
        return knowledge;
    }

    static void replace(PlayerKnowledge newKnowledge) {
        knowledge = newKnowledge;
    }

    static void clear() {
        knowledge = PlayerKnowledge.EMPTY;
    }
}
