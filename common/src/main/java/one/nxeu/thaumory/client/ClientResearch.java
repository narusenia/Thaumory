package one.nxeu.thaumory.client;

import one.nxeu.thaumory.research.ResearchView;

/** The chapters the server last said this player can see. */
public final class ClientResearch {
    private static volatile ResearchView view = ResearchView.EMPTY;

    private ClientResearch() {}

    public static ResearchView get() {
        return view;
    }

    static void replace(ResearchView newView) {
        view = newView;
    }

    static void clear() {
        view = ResearchView.EMPTY;
    }
}
