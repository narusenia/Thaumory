package one.nxeu.thaumory.circle.effect;

import one.nxeu.thaumory.api.infusion.InfusionEffectRegistry;

/** What Thaumory's circle effects do once burnt into an item (requirements §10.2), registered through the API. */
public final class ThaumoryInfusionEffects {
    private ThaumoryInfusionEffects() {}

    public static void register(InfusionEffectRegistry registry) {
        registry.register(ThaumoryCircleEffects.HEALING, new HealingInfusion());
        registry.register(ThaumoryCircleEffects.WARD, new WardInfusion());
        registry.register(ThaumoryCircleEffects.ATTRACTION, new AttractionInfusion());
        registry.register(ThaumoryCircleEffects.GROWTH, new GrowthInfusion());
        registry.register(ThaumoryCircleEffects.LIGHT, new LightInfusion());
        registry.register(ThaumoryCircleEffects.TELEPORT, new TeleportInfusion());
        registry.register(ThaumoryCircleEffects.WEATHER, new WeatherInfusion());
        registry.register(ThaumoryCircleEffects.PURIFICATION, new PurificationInfusion());
    }
}
