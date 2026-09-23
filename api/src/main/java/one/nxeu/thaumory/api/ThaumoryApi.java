package one.nxeu.thaumory.api;

import one.nxeu.thaumory.api.aspect.AspectRegistry;

/** Entry point for addons. */
public final class ThaumoryApi {
    public static final String MOD_ID = "thaumory";

    private static final AspectRegistry ASPECTS = new AspectRegistry();

    private ThaumoryApi() {}

    public static AspectRegistry aspects() {
        return ASPECTS;
    }
}
