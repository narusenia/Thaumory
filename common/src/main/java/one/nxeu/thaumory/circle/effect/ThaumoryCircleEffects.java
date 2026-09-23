package one.nxeu.thaumory.circle.effect;

import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.circle.CircleEffectRegistry;

/** Thaumory's own circle effects, registered through the API like an addon's (requirements §4.5). */
public final class ThaumoryCircleEffects {
    public static final Identifier TELEPORT = Thaumory.id("teleport");
    public static final Identifier LIGHT = Thaumory.id("light");
    public static final Identifier PURIFICATION = Thaumory.id("purification");
    public static final Identifier WARD = Thaumory.id("ward");
    public static final Identifier GROWTH = Thaumory.id("growth");
    public static final Identifier HEALING = Thaumory.id("healing");
    public static final Identifier ATTRACTION = Thaumory.id("attraction");
    public static final Identifier WEATHER = Thaumory.id("weather");

    private ThaumoryCircleEffects() {}

    public static void register(CircleEffectRegistry registry) {
        registry.register(TELEPORT, new TeleportEffect());
        registry.register(LIGHT, new LightEffect());
        registry.register(PURIFICATION, new PurificationEffect());
        registry.register(WARD, new WardEffect());
        registry.register(GROWTH, new GrowthEffect());
        registry.register(HEALING, new HealingEffect());
        registry.register(ATTRACTION, new AttractionEffect());
        registry.register(WEATHER, new WeatherEffect());
        WardEffect.registerEvents();
    }
}
