package one.nxeu.thaumory.circle.effect;

import net.minecraft.world.effect.MobEffects;
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
        registry.register(ThaumoryCircleEffects.LIGHTNESS, new AuraInfusion(true,
                new AuraEffect.Aura(MobEffects.SPEED, ThaumoryCircleEffects.SHORT_AURA, 0, true)));
        registry.register(ThaumoryCircleEffects.BREATH, new AuraInfusion(false,
                new AuraEffect.Aura(MobEffects.WATER_BREATHING, ThaumoryCircleEffects.SHORT_AURA, 0, false),
                new AuraEffect.Aura(MobEffects.DOLPHINS_GRACE, ThaumoryCircleEffects.SHORT_AURA, 0, false)));
        registry.register(ThaumoryCircleEffects.NIGHT_SIGHT, new AuraInfusion(false,
                new AuraEffect.Aura(MobEffects.NIGHT_VISION, ThaumoryCircleEffects.NIGHT_VISION_AURA, 0, false)));
        registry.register(ThaumoryCircleEffects.BINDING, new AfflictionInfusion(2, new AuraEffect.Aura(MobEffects.SLOWNESS, 0, 3, true)));
        registry.register(ThaumoryCircleEffects.WITHERING, new AfflictionInfusion(3,
                new AuraEffect.Aura(MobEffects.WITHER, 0, 0, false), new AuraEffect.Aura(MobEffects.WEAKNESS, 0, 0, false)));
        registry.register(ThaumoryCircleEffects.SEARING, new SearingInfusion());
        registry.register(ThaumoryCircleEffects.MINING, new MiningInfusion());
        AuraInfusion.registerEvents();
    }
}
