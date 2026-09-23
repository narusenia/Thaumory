package one.nxeu.thaumory.crucible;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.METALLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import org.junit.jupiter.api.Test;

class CrucibleTankTest {
    private static final CrucibleSettings SETTINGS = CrucibleSettings.DEFAULT;
    private static final CrucibleTank FULL_OF_WATER = CrucibleTank.EMPTY.withWater(3);
    private static final AspectList LOG = AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 4));

    @Test
    void meltsAllOfAManualItem() {
        CrucibleTank.MeltResult result = FULL_OF_WATER.melt(LOG, 1.0, SETTINGS);

        assertEquals(LOG, result.tank().contents());
        assertEquals(0, result.overflow());
    }

    @Test
    void roundsEachAspectDownForEstimatedItems() {
        AspectList planks = AspectList.of(new AspectStack(HERBA, 4), new AspectStack(TERRA, 1));
        CrucibleTank.MeltResult result = FULL_OF_WATER.melt(planks, 0.75, SETTINGS);

        // 4 * 0.75 = 3; 1 * 0.75 rounds down to nothing.
        assertEquals(AspectList.of(HERBA, 3), result.tank().contents());
        assertEquals(0, result.overflow());
    }

    @Test
    void mixesWithWhatIsAlreadyThere() {
        CrucibleTank tank = FULL_OF_WATER.melt(LOG, 1.0, SETTINGS).tank()
                .melt(AspectList.of(TERRA, 6), 1.0, SETTINGS).tank();

        assertEquals(AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 10)), tank.contents());
    }

    @Test
    void overflowsWhatDoesNotFitLargestFirst() {
        CrucibleSettings small = new CrucibleSettings(20, SETTINGS.meltRatio(), 0, 1, 1000, 20, 1);
        CrucibleTank tank = FULL_OF_WATER.melt(AspectList.of(AQUA, 10), 1.0, small).tank();

        CrucibleTank.MeltResult result = tank.melt(LOG, 1.0, small);

        // 10 of room: Herba (the largest) fills it, Terra overflows entirely.
        assertEquals(AspectList.of(new AspectStack(AQUA, 10), new AspectStack(HERBA, 10)), result.tank().contents());
        assertEquals(6 + 4, result.overflow());
    }

    @Test
    void overflowsEverythingWhenFull() {
        CrucibleSettings small = new CrucibleSettings(20, SETTINGS.meltRatio(), 0, 1, 1000, 20, 1);
        CrucibleTank full = FULL_OF_WATER.melt(LOG, 1.0, small).tank();

        CrucibleTank.MeltResult result = full.melt(LOG, 1.0, small);
        assertEquals(20, result.overflow());
        assertEquals(full.contents(), result.tank().contents());
    }

    @Test
    void usesOneWaterLevelPerHundredEssentia() {
        AspectList ingot = AspectList.of(METALLUM, 60);
        CrucibleTank once = FULL_OF_WATER.melt(ingot, 1.0, SETTINGS).tank();
        assertEquals(3, once.water());
        assertEquals(60, once.essentiaSinceWaterDrop());

        CrucibleTank twice = once.melt(ingot, 1.0, SETTINGS).tank();
        assertEquals(2, twice.water());
        assertEquals(20, twice.essentiaSinceWaterDrop());
    }

    @Test
    void overflowStillUsesWater() {
        CrucibleSettings small = new CrucibleSettings(10, SETTINGS.meltRatio(), 0, 1, 20, 20, 1);
        CrucibleTank.MeltResult result = FULL_OF_WATER.melt(LOG, 1.0, small);

        assertEquals(10, result.overflow());
        assertEquals(2, result.tank().water());
    }

    @Test
    void runsDryAndKeepsContents() {
        CrucibleTank tank = CrucibleTank.EMPTY.withWater(1).melt(AspectList.of(METALLUM, 250), 1.0, SETTINGS).tank();

        assertEquals(0, tank.water());
        assertEquals(0, tank.essentiaSinceWaterDrop());
        assertEquals(AspectList.of(METALLUM, 250), tank.contents());
        assertTrue(!tank.hasWater());
    }

    @Test
    void cancelsOppositesAndKeepsWater() {
        CrucibleTank tank = new CrucibleTank(AspectList.of(new AspectStack(AQUA, 5), new AspectStack(IGNIS, 3), new AspectStack(TERRA, 7)), 2, 40);

        CrucibleTank.CancelResult result = tank.cancel(registry(), SETTINGS);

        assertEquals(AspectList.of(new AspectStack(AQUA, 4), new AspectStack(IGNIS, 2), new AspectStack(TERRA, 7)), result.tank().contents());
        assertEquals(2, result.flux());
        assertEquals(2, result.tank().water());
        assertEquals(40, result.tank().essentiaSinceWaterDrop());
    }

    private static AspectRegistry registry() {
        AspectRegistry registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
        return registry;
    }

    @Test
    void roundTripsThroughCodec() {
        AspectRegistry registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
        CrucibleTank tank = new CrucibleTank(LOG, 2, 40);

        var json = CrucibleTank.codec(AspectCodecs.aspectList(registry)).encodeStart(JsonOps.INSTANCE, tank).getOrThrow();
        assertEquals(tank, CrucibleTank.codec(AspectCodecs.aspectList(registry)).parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void parsesSettings() {
        CrucibleSettings settings = CrucibleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                { "capacity": 500, "melt_ratio": { "manual": 1.0, "estimated": 0.75 },
                  "boil_time": 100, "melt_interval": 5, "essentia_per_water_level": 100,
                  "cancel_interval": 20, "cancel_amount": 1 }""")).getOrThrow();
        assertEquals(CrucibleSettings.DEFAULT, settings);
    }
}
