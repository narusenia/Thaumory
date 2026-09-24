package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
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
    public static final Identifier CHARGING = Thaumory.id("charging");
    public static final Identifier HARVEST = Thaumory.id("harvest");
    public static final Identifier BREEDING = Thaumory.id("breeding");
    public static final Identifier MOISTURE = Thaumory.id("moisture");
    public static final Identifier SMELTING = Thaumory.id("smelting");
    public static final Identifier MINING = Thaumory.id("mining");
    public static final Identifier SORTING = Thaumory.id("sorting");
    public static final Identifier MELTING = Thaumory.id("melting");
    public static final Identifier LIGHTNESS = Thaumory.id("lightness");
    public static final Identifier BREATH = Thaumory.id("breath");
    public static final Identifier NIGHT_SIGHT = Thaumory.id("night_sight");
    public static final Identifier SAFEGUARD = Thaumory.id("safeguard");
    public static final Identifier LURE = Thaumory.id("lure");
    public static final Identifier BINDING = Thaumory.id("binding");
    public static final Identifier SEARING = Thaumory.id("searing");
    public static final Identifier WITHERING = Thaumory.id("withering");

    /** How long the life circles' status effects outlast leaving the range, in ticks (requirements §17.4). */
    private static final int SHORT_AURA = 60;
    /** Night vision flickers under 10 seconds left, so it is kept above that. */
    private static final int NIGHT_VISION_AURA = 220;
    /**
     * Wither hurts on durations that are multiples of 40; renewed to 80 every second, it bites once
     * a second.
     */
    private static final int WITHER_AURA = 80;

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
        registry.register(CHARGING, new ChargingEffect());
        registry.register(HARVEST, new HarvestEffect());
        registry.register(BREEDING, new BreedingEffect());
        registry.register(MOISTURE, new MoistureEffect());
        registry.register(SMELTING, new SmeltingEffect());
        registry.register(MINING, new MiningEffect());
        registry.register(SORTING, new SortingEffect());
        registry.register(MELTING, new MeltingEffect());
        registry.register(LIGHTNESS, new AuraEffect(AuraEffect::playersUnlessPicked, true,
                new AuraEffect.Aura(MobEffects.SPEED, SHORT_AURA, 0, true)));
        registry.register(BREATH, new AuraEffect(AuraEffect::playersUnlessPicked, false,
                new AuraEffect.Aura(MobEffects.WATER_BREATHING, SHORT_AURA, 0, false),
                new AuraEffect.Aura(MobEffects.DOLPHINS_GRACE, SHORT_AURA, 0, false)));
        registry.register(NIGHT_SIGHT, new AuraEffect(AuraEffect::playersUnlessPicked, false,
                new AuraEffect.Aura(MobEffects.NIGHT_VISION, NIGHT_VISION_AURA, 0, false)));
        registry.register(SAFEGUARD, new SafeguardEffect());
        registry.register(LURE, new LureEffect());
        // Slowness IV at strength 1.
        registry.register(BINDING, new AuraEffect(AuraEffect::enemiesUnlessPicked, false,
                new AuraEffect.Aura(MobEffects.SLOWNESS, SHORT_AURA, 3, true)));
        registry.register(SEARING, new SearingEffect());
        registry.register(WITHERING, new AuraEffect(parameter -> AuraEffect.enemiesUnlessPicked(Optional.empty()), false,
                new AuraEffect.Aura(MobEffects.WITHER, WITHER_AURA, 0, false),
                new AuraEffect.Aura(MobEffects.WEAKNESS, WITHER_AURA, 0, false)));
        WardEffect.registerEvents();
        AuraEffect.registerEvents();
        SafeguardEffect.registerEvents();
    }
}
