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
    public static final Identifier CHARGING = Thaumory.id("charging");
    public static final Identifier HARVEST = Thaumory.id("harvest");
    public static final Identifier BREEDING = Thaumory.id("breeding");
    public static final Identifier MOISTURE = Thaumory.id("moisture");
    public static final Identifier SMELTING = Thaumory.id("smelting");
    public static final Identifier MINING = Thaumory.id("mining");
    public static final Identifier SORTING = Thaumory.id("sorting");
    public static final Identifier MELTING = Thaumory.id("melting");

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
        WardEffect.registerEvents();
    }
}
