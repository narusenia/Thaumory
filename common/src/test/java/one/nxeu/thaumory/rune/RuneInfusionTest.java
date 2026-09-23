package one.nxeu.thaumory.rune;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.Test;

class RuneInfusionTest {
    private static final int COST = 16;

    @Test
    void unlabeledJarGivesItsLargestAspect() {
        AspectList jar = AspectList.of(new AspectStack(HERBA, 40), new AspectStack(TERRA, 20));

        RuneInfusion.Result result = RuneInfusion.infuse(jar, Optional.empty(), COST).orElseThrow();

        assertEquals(HERBA, result.aspect());
        assertEquals(AspectList.of(new AspectStack(HERBA, 24), new AspectStack(TERRA, 20)), result.remaining());
    }

    @Test
    void labeledJarGivesItsLabelsAspect() {
        RuneInfusion.Result result = RuneInfusion.infuse(AspectList.of(IGNIS, 16), Optional.of(IGNIS), COST).orElseThrow();

        assertEquals(IGNIS, result.aspect());
        assertTrue(result.remaining().isEmpty());
    }

    @Test
    void tooLittleTakesNothing() {
        AspectList jar = AspectList.of(new AspectStack(HERBA, 15), new AspectStack(TERRA, 30));

        // Herba is not the largest, and Terra is not what the label asks for: neither is used instead.
        assertTrue(RuneInfusion.infuse(jar, Optional.of(HERBA), COST).isEmpty());
        assertTrue(RuneInfusion.infuse(AspectList.of(AQUA, 15), Optional.empty(), COST).isEmpty());
    }

    @Test
    void emptyJarGivesNothing() {
        assertTrue(RuneInfusion.infuse(AspectList.empty(), Optional.empty(), COST).isEmpty());
        assertTrue(RuneInfusion.infuse(AspectList.empty(), Optional.of(AQUA), COST).isEmpty());
    }

    @Test
    void settingsReadTheCost() {
        RuneSettings settings = RuneSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"cost\": 8}")).getOrThrow();
        assertEquals(8, settings.cost());
        assertTrue(RuneSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"cost\": 0}")).isError());
    }
}
