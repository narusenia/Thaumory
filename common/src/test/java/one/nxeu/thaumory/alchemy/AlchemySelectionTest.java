package one.nxeu.thaumory.alchemy;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ORDO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import org.junit.jupiter.api.Test;

class AlchemySelectionTest {
    private static final AlchemySelection.Candidate<String> AMPLIFYING =
            new AlchemySelection.Candidate<>(Identifier.parse("thaumory:amplifying_chalk"), AspectList.of(IGNIS, 8), "amplifying");
    private static final AlchemySelection.Candidate<String> EXTENDING =
            new AlchemySelection.Candidate<>(Identifier.parse("thaumory:extending_chalk"), AspectList.of(AER, 8), "extending");
    private static final AlchemySelection.Candidate<String> STABILIZING =
            new AlchemySelection.Candidate<>(Identifier.parse("thaumory:stabilizing_chalk"), AspectList.of(ORDO, 4), "stabilizing");

    @Test
    void choosesNothingWhenNoRecipeIsPaidFor() {
        assertEquals(Optional.empty(), AlchemySelection.choose(List.of(AMPLIFYING, EXTENDING), AspectList.of(IGNIS, 7)));
    }

    @Test
    void choosesTheOnlyRecipeThatIsPaidFor() {
        AspectList contents = AspectList.of(new AspectStack(IGNIS, 10), new AspectStack(AER, 3));
        assertEquals("amplifying", AlchemySelection.choose(List.of(AMPLIFYING, EXTENDING), contents).orElseThrow().recipe());
    }

    @Test
    void prefersTheRecipeNeedingMoreEssentia() {
        AspectList contents = AspectList.of(new AspectStack(IGNIS, 10), new AspectStack(ORDO, 10));
        assertEquals("amplifying", AlchemySelection.choose(List.of(STABILIZING, AMPLIFYING), contents).orElseThrow().recipe());
    }

    @Test
    void breaksTiesByRecipeId() {
        AspectList contents = AspectList.of(new AspectStack(IGNIS, 10), new AspectStack(AER, 10));
        assertEquals("amplifying", AlchemySelection.choose(List.of(EXTENDING, AMPLIFYING), contents).orElseThrow().recipe());
    }

    @Test
    void strictCodecRejectsUnknownAspects() {
        AspectRegistry registry = new AspectRegistry();
        ThaumoryAspects.register(registry);

        assertEquals(AspectList.of(IGNIS, 8), AspectCodecs.strictAspectList(registry)
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"thaumory:ignis\": 8 }")).getOrThrow());
        assertTrue(AspectCodecs.strictAspectList(registry)
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"thaumory:ignis\": 8, \"thaumory:nope\": 1 }")).isError());
    }
}
