package one.nxeu.thaumory.circle;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.UMBRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.jar.EssentiaTransfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CircleRulesTest {
    private static final Identifier AMPLIFY = Identifier.fromNamespaceAndPath("thaumory", "amplifying_pattern");
    private static final Identifier EXTEND = Identifier.fromNamespaceAndPath("thaumory", "extending_pattern");
    private static final Identifier ECONOMIZE = Identifier.fromNamespaceAndPath("thaumory", "economizing_pattern");
    private static final Identifier LIGHT = Identifier.fromNamespaceAndPath("thaumory", "light");
    private static final Identifier TELEPORT = Identifier.fromNamespaceAndPath("thaumory", "teleport");

    private AspectRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
    }

    private static List<CircleScan.Node> nodes(Identifier... patterns) {
        List<CircleScan.Node> nodes = new ArrayList<>();
        for (int i = 0; i < patterns.length; i++) {
            nodes.add(new CircleScan.Node(1 + i / 4, CircleSide.values()[i % 4], patterns[i]));
        }
        return nodes;
    }

    @Test
    void multipliersAddUpFromOne() {
        CircleSettings.Multipliers m = CircleSettings.DEFAULT.multipliers(nodes(AMPLIFY, AMPLIFY, ECONOMIZE));
        assertEquals(1.75, m.strength(), 1e-9);
        assertEquals(1.0, m.range(), 1e-9);
        assertEquals(1.75, m.cost(), 1e-9);
        assertEquals(CircleSettings.Multipliers.NONE, CircleSettings.DEFAULT.multipliers(List.of()));
    }

    @Test
    void multipliersNeverGoBelowAQuarter() {
        CircleSettings.Multipliers m = CircleSettings.DEFAULT.multipliers(nodes(ECONOMIZE, ECONOMIZE, ECONOMIZE, ECONOMIZE, ECONOMIZE));
        assertEquals(0.25, m.cost(), 1e-9);
        assertEquals(0.25, m.strength(), 1e-9);
    }

    @Test
    void radiusFollowsRingsAndRange() {
        CircleSettings settings = CircleSettings.DEFAULT;
        assertEquals(0, settings.radius(0, CircleSettings.Multipliers.NONE), 1e-9);
        assertEquals(4, settings.radius(1, CircleSettings.Multipliers.NONE), 1e-9);
        assertEquals(16, settings.radius(3, CircleSettings.Multipliers.NONE), 1e-9);
        assertEquals(12, settings.radius(2, settings.multipliers(nodes(EXTEND))), 1e-9);
    }

    @Test
    void settingsReadRadiusAndCapacity() {
        CircleSettings settings = CircleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"scan_interval": 40, "instability_threshold": 3, "ring_radius": [2, 3, 5], "essentia_capacity": 10}
                """)).getOrThrow();
        assertEquals(List.of(2, 3, 5), settings.ringRadius());
        assertEquals(10, settings.essentiaCapacity());
        assertTrue(CircleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"scan_interval\": 40, \"instability_threshold\": 3, \"ring_radius\": [2, 3]}")).isError());
    }

    private CircleDefinitions definitions(String... jsonById) {
        Map<Identifier, CircleDefinitionFile> files = new java.util.HashMap<>();
        for (int i = 0; i < jsonById.length; i += 2) {
            files.put(Identifier.parse(jsonById[i]),
                    CircleDefinitionFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(jsonById[i + 1])).getOrThrow());
        }
        return CircleDefinitions.build(files, registry, Set.of(LIGHT, TELEPORT)::contains, warning -> {});
    }

    @Test
    void combinationsMatchEitherOrderAndTheirSlot3() {
        CircleDefinitions table = definitions("thaumory:light", """
                {"effect": "thaumory:light", "runes": ["thaumory:lux", "thaumory:ignis"], "slot3": ["none", "thaumory:umbra"],
                 "mode": "sustained", "interval": 200}""");

        CircleDefinitions.Definition light = table.find(IGNIS, LUX, Optional.empty()).orElseThrow();
        assertEquals(LIGHT, light.effect());
        assertEquals(CircleMode.SUSTAINED, light.mode());
        assertEquals(200, light.interval());
        assertTrue(table.find(LUX, IGNIS, Optional.of(UMBRA)).isPresent());
        assertTrue(table.find(LUX, IGNIS, Optional.of(AER)).isEmpty(), "Aer is not an accepted slot 3");
        assertTrue(table.find(LUX, AER, Optional.empty()).isEmpty());
    }

    @Test
    void anySlot3NeedsARune() {
        CircleDefinitions table = definitions("thaumory:teleport", """
                {"effect": "thaumory:teleport", "runes": ["thaumory:arcanum", "thaumory:aer"], "slot3": "any",
                 "mode": "triggered", "cost": 4}""");

        assertEquals(4, table.find(AER, ARCANUM, Optional.of(LUX)).orElseThrow().cost());
        assertTrue(table.find(AER, ARCANUM, Optional.empty()).isEmpty());
    }

    @Test
    void anyInAListAlsoAllowsTheListedEntries() {
        CircleDefinitions table = definitions("thaumory:attraction", """
                {"effect": "thaumory:light", "runes": ["thaumory:lux", "thaumory:ignis"], "slot3": ["none", "any"],
                 "mode": "sustained", "settings": {"speed": 0.3}}""");

        assertTrue(table.find(LUX, IGNIS, Optional.empty()).isPresent());
        CircleDefinitions.Definition definition = table.find(LUX, IGNIS, Optional.of(AER)).orElseThrow();
        assertEquals(Map.of("speed", 0.3), definition.settings());
    }

    @Test
    void laterFileWinsAndBrokenFilesAreSkipped() {
        List<String> warnings = new ArrayList<>();
        Map<Identifier, CircleDefinitionFile> files = Map.of(
                Identifier.parse("a:first"), file("thaumory:light", 1),
                Identifier.parse("b:second"), file("thaumory:light", 2),
                Identifier.parse("c:unknown_effect"), file("thaumory:nothing", 3));
        CircleDefinitions table = CircleDefinitions.build(files, registry, Set.of(LIGHT)::contains, warnings::add);

        assertEquals(2, table.size());
        assertEquals(Identifier.parse("b:second"), table.find(LUX, IGNIS, Optional.empty()).orElseThrow().id());
        assertEquals(1, warnings.size());
    }

    private static CircleDefinitionFile file(String effect, int cost) {
        return CircleDefinitionFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"effect\": \"" + effect + "\", \"runes\": [\"thaumory:lux\", \"thaumory:ignis\"], \"mode\": \"triggered\", \"cost\": " + cost + "}"))
                .getOrThrow();
    }

    @Test
    void triggeredCostScalesAndRounds() {
        assertEquals(4, CircleUpkeep.triggeredCost(4, 1.0));
        assertEquals(7, CircleUpkeep.triggeredCost(4, 1.75));
        assertEquals(1, CircleUpkeep.triggeredCost(1, 0.25));
    }

    @Test
    void sustainedIntervalStretchesWhenCheaper() {
        assertEquals(200, CircleUpkeep.sustainedInterval(200, 1.0));
        assertEquals(267, CircleUpkeep.sustainedInterval(200, 0.75));
        assertEquals(100, CircleUpkeep.sustainedInterval(200, 2.0));
    }

    @Test
    void triggeredPaysBothRunesAndOneFromSlot3() {
        AspectList stored = AspectList.of(new AspectStack(ARCANUM, 10), new AspectStack(AER, 4), new AspectStack(LUX, 2));

        assertEquals(AspectList.of(new AspectStack(ARCANUM, 6), new AspectStack(LUX, 1)),
                CircleUpkeep.payTriggered(stored, ARCANUM, AER, Optional.of(LUX), 4).orElseThrow());
        assertTrue(CircleUpkeep.payTriggered(stored, ARCANUM, AER, Optional.of(LUX), 5).isEmpty(), "Aer runs short");
    }

    @Test
    void sustainedPaysOneEachAndStopsWhenShort() {
        AspectList stored = AspectList.of(new AspectStack(LUX, 1), new AspectStack(IGNIS, 3));

        AspectList after = CircleUpkeep.paySustained(stored, LUX, IGNIS).orElseThrow();
        assertEquals(AspectList.of(IGNIS, 2), after);
        assertTrue(CircleUpkeep.paySustained(after, LUX, IGNIS).isEmpty());
    }

    @Test
    void sameAspectInBothSlotsPaysTwice() {
        assertTrue(CircleUpkeep.paySustained(AspectList.of(LUX, 1), LUX, LUX).isEmpty());
        assertEquals(AspectList.empty(), CircleUpkeep.paySustained(AspectList.of(LUX, 2), LUX, LUX).orElseThrow());
    }

    @Test
    void coreTakesOnlyItsRuneAspectsUpToEachCapacity() {
        AspectList jar = AspectList.of(new AspectStack(LUX, 40), new AspectStack(UMBRA, 10), new AspectStack(AER, 5));
        AspectList core = AspectList.of(LUX, 30);

        EssentiaTransfer.Result result = EssentiaTransfer.pourSeparated(jar, core, Set.of(LUX, UMBRA), 64);

        // Lux and Umbra are opposites but sit side by side; Aer is not a rune and stays in the jar.
        assertEquals(AspectList.of(new AspectStack(LUX, 64), new AspectStack(UMBRA, 10)), result.to());
        assertEquals(AspectList.of(new AspectStack(LUX, 6), new AspectStack(AER, 5)), result.from());
        assertEquals(0, result.flux());
    }
}
