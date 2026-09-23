package one.nxeu.thaumory.circle.effect;

import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.circle.CircleEffectRegistry;

/** Thaumory's own circle effects, registered through the API like an addon's (requirements §4.5). */
public final class ThaumoryCircleEffects {
    public static final Identifier LIGHT = Thaumory.id("light");

    private ThaumoryCircleEffects() {}

    public static void register(CircleEffectRegistry registry) {
        registry.register(LIGHT, new LightEffect());
    }
}
