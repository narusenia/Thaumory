package one.nxeu.thaumory.infusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class CapacityTableTest {
    private static final Identifier SWORD = Identifier.parse("thaumory:arcane_iron_sword");
    private static final Identifier HOE = Identifier.parse("thaumory:arcane_iron_hoe");
    private static final Identifier IRON_SWORD = Identifier.parse("minecraft:iron_sword");
    private static final Identifier ARCANE_IRON = Identifier.parse("thaumory:arcane_iron_equipment");
    private static final Identifier SWORDS = Identifier.parse("minecraft:swords");
    private static final Set<Identifier> KNOWN = Set.of(SWORD, HOE, IRON_SWORD);

    private final List<String> warnings = new ArrayList<>();

    @Test
    void theItemsOwnEntryWinsOverTags() {
        CapacityTable table = build(file("""
                { "values": [
                  { "target": "thaumory:arcane_iron_hoe", "capacity": 0 },
                  { "target": "#thaumory:arcane_iron_equipment", "capacity": 3 }
                ] }"""));

        assertEquals(3, table.capacity(SWORD, tags(ARCANE_IRON)));
        assertEquals(0, table.capacity(HOE, tags(ARCANE_IRON)));
    }

    @Test
    void theTagLoadedLastWins() {
        CapacityTable table = build(
                file("""
                        { "values": [ { "target": "#thaumory:arcane_iron_equipment", "capacity": 3 } ] }"""),
                file("""
                        { "values": [ { "target": "#minecraft:swords", "capacity": 1 } ] }"""));

        assertEquals(1, table.capacity(SWORD, tags(ARCANE_IRON, SWORDS)));
        assertEquals(3, table.capacity(HOE, tags(ARCANE_IRON)));
    }

    @Test
    void anItemNoEntryCoversHasNoCapacity() {
        CapacityTable table = build(file("""
                { "values": [ { "target": "#thaumory:arcane_iron_equipment", "capacity": 3 } ] }"""));

        assertEquals(0, table.capacity(IRON_SWORD, tags(SWORDS)));
    }

    @Test
    void unknownItemsAreSkippedWithAWarning() {
        CapacityTable table = build(file("""
                { "values": [ { "target": "othermod:missing", "capacity": 2 } ] }"""));

        assertEquals(0, table.size());
        assertTrue(warnings.getFirst().contains("othermod:missing"));
    }

    private CapacityTable build(CapacityFile... files) {
        List<Map.Entry<Identifier, CapacityFile>> entries = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            entries.add(Map.entry(Identifier.parse("test:file_" + i), files[i]));
        }
        return CapacityTable.build(entries, KNOWN::contains, warnings::add);
    }

    private static CapacityFile file(String json) {
        return CapacityFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static Predicate<Identifier> tags(Identifier... tags) {
        return Set.of(tags)::contains;
    }
}
