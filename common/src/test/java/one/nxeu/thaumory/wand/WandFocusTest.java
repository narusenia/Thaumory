package one.nxeu.thaumory.wand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class WandFocusTest {
    private static final Identifier LIGHT = Identifier.fromNamespaceAndPath("thaumory", "light_focus");
    private static final Identifier LUX = Identifier.fromNamespaceAndPath("thaumory", "lux");

    private static DataResult<WandFocus> parse(String json) {
        return WandFocus.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    @Test
    void readsAFocusFile() {
        WandFocus focus = parse("""
                {"item": "thaumory:light_focus", "effect": "thaumory:light", "cost": {"thaumory:lux": 1}, "cooldown": 10, "settings": {"range": 32}}
                """).getOrThrow();
        assertEquals(LIGHT, focus.item());
        assertEquals(Map.of(LUX, 1), focus.cost());
        assertEquals(Set.of(LUX), focus.aspects());
        assertEquals(10, focus.cooldown());
        assertEquals(32, focus.setting("range", 8));
        assertEquals(8, focus.setting("radius", 8));
    }

    @Test
    void theCooldownAndSettingsMayBeLeftOut() {
        WandFocus focus = parse("""
                {"item": "thaumory:light_focus", "effect": "thaumory:light", "cost": {"thaumory:lux": 1}}
                """).getOrThrow();
        assertEquals(0, focus.cooldown());
        assertEquals(Map.of(), focus.settings());
    }

    @Test
    void aFocusMustCostSomething() {
        assertTrue(parse("""
                {"item": "thaumory:light_focus", "effect": "thaumory:light", "cost": {}}
                """).isError());
    }

    @Test
    void aLaterFileForTheSameItemWins() {
        WandFocus first = new WandFocus(LIGHT, Identifier.fromNamespaceAndPath("thaumory", "light"), Map.of(LUX, 1), 10, Map.of());
        WandFocus second = new WandFocus(LIGHT, Identifier.fromNamespaceAndPath("thaumory", "light"), Map.of(LUX, 2), 10, Map.of());
        assertEquals(Map.of(LIGHT, second), WandFoci.table(List.of(first, second)));
    }
}
