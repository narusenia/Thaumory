package one.nxeu.thaumory;

import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

public final class Thaumory {
    public static final String MOD_ID = ThaumoryApi.MOD_ID;

    public static void init() {
        ThaumoryAspects.register(ThaumoryApi.aspects());
    }
}
