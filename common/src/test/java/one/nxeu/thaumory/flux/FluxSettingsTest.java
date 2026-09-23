package one.nxeu.thaumory.flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import one.nxeu.thaumory.api.flux.FluxStage;
import org.junit.jupiter.api.Test;

class FluxSettingsTest {
    private static final FluxSettings SETTINGS = FluxSettings.DEFAULT;
    private static final long HOUR = 72_000;

    @Test
    void losesTheFractionEveryInterval() {
        assertEquals(90, SETTINGS.decay(100, HOUR), 1e-9);
        assertEquals(81, SETTINGS.decay(100, 2 * HOUR), 1e-9);
    }

    @Test
    void decaysContinuouslyWithinAnInterval() {
        assertEquals(100 * Math.sqrt(0.9), SETTINGS.decay(100, HOUR / 2), 1e-9);
    }

    @Test
    void doesNotDecayWhenNoTimePassesOrTimeGoesBack() {
        assertEquals(100, SETTINGS.decay(100, 0));
        assertEquals(100, SETTINGS.decay(100, -20));
    }

    @Test
    void stagesStartAtTheirThresholds() {
        assertEquals(FluxStage.NONE, SETTINGS.stage(0));
        assertEquals(FluxStage.NONE, SETTINGS.stage(49.99));
        assertEquals(FluxStage.STAGNATION, SETTINGS.stage(50));
        assertEquals(FluxStage.STAGNATION, SETTINGS.stage(149.5));
        assertEquals(FluxStage.EROSION, SETTINGS.stage(150));
        assertEquals(FluxStage.MANIFESTATION, SETTINGS.stage(300));
        assertEquals(FluxStage.OVERLOAD, SETTINGS.stage(500));
        assertEquals(FluxStage.OVERLOAD, SETTINGS.stage(1e9));
    }

    @Test
    void parsesFile() {
        FluxSettings settings = parse("""
                { "decay": { "fraction": 0.5, "interval": 100 },
                  "stages": { "stagnation": 1, "erosion": 2, "manifestation": 3, "overload": 4 } }""").getOrThrow();

        assertEquals(new FluxSettings(new FluxSettings.Decay(0.5, 100), new FluxSettings.Stages(1, 2, 3, 4), FluxSettings.Effects.DEFAULT), settings);
        assertEquals(25, settings.decay(100, 200), 1e-9);
        assertEquals(FluxStage.MANIFESTATION, settings.stage(3));
    }

    @Test
    void effectsGrowWithTheStage() {
        FluxSettings.Effects effects = FluxSettings.Effects.DEFAULT;
        assertEquals(0, effects.particles(FluxStage.NONE));
        assertEquals(2, effects.particles(FluxStage.STAGNATION));
        assertEquals(10, effects.particles(FluxStage.OVERLOAD));
        assertEquals(0, effects.pollutionAttempts(FluxStage.STAGNATION));
        assertEquals(1, effects.pollutionAttempts(FluxStage.EROSION));
        assertEquals(3, effects.pollutionAttempts(FluxStage.OVERLOAD));
        assertEquals(0, effects.extraInstability(FluxStage.EROSION));
        assertEquals(3, effects.extraInstability(FluxStage.MANIFESTATION));
        assertEquals(3, effects.extraInstability(FluxStage.OVERLOAD));
        assertEquals(0, effects.misfireChance(FluxStage.MANIFESTATION), 1e-9);
        assertEquals(0.25, effects.misfireChance(FluxStage.OVERLOAD), 1e-9);
        assertTrue(effects.stopsCrops(FluxStage.EROSION) && !effects.stopsCrops(FluxStage.STAGNATION));
        assertTrue(effects.spawnsRemnants(FluxStage.MANIFESTATION) && !effects.spawnsRemnants(FluxStage.EROSION));
    }

    @Test
    void effectsFallBackPerValueAndCheckListLengths() {
        FluxSettings settings = parse("""
                { "decay": { "fraction": 0.1, "interval": 72000 },
                  "stages": { "stagnation": 50, "erosion": 150, "manifestation": 300, "overload": 500 },
                  "effects": { "pollution_attempts": [0, 5, 9], "misfire_chance": 0.5 } }""").getOrThrow();
        assertEquals(List.of(0, 5, 9), settings.effects().pollutionAttempts());
        assertEquals(0.5, settings.effects().misfireChance(), 1e-9);
        assertEquals(FluxSettings.Effects.DEFAULT.particles(), settings.effects().particles());
        assertTrue(parse("""
                { "decay": { "fraction": 0.1, "interval": 72000 },
                  "stages": { "stagnation": 50, "erosion": 150, "manifestation": 300, "overload": 500 },
                  "effects": { "particles": [1, 2] } }""").isError());
    }

    @Test
    void roundTripsDefault() {
        var json = FluxSettings.CODEC.encodeStart(JsonOps.INSTANCE, FluxSettings.DEFAULT).getOrThrow();
        assertEquals(FluxSettings.DEFAULT, FluxSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void rejectsStagesOutOfOrder() {
        assertTrue(parse("""
                { "decay": { "fraction": 0.1, "interval": 72000 },
                  "stages": { "stagnation": 50, "erosion": 300, "manifestation": 150, "overload": 500 } }""").isError());
    }

    @Test
    void rejectsBadDecay() {
        assertTrue(parse("""
                { "decay": { "fraction": 1.5, "interval": 72000 },
                  "stages": { "stagnation": 50, "erosion": 150, "manifestation": 300, "overload": 500 } }""").isError());
        assertTrue(parse("""
                { "decay": { "fraction": 0.1, "interval": 0 },
                  "stages": { "stagnation": 50, "erosion": 150, "manifestation": 300, "overload": 500 } }""").isError());
    }

    private static DataResult<FluxSettings> parse(String json) {
        return FluxSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
