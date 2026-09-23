package one.nxeu.thaumory.aspect.data;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BESTIA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemAspectTableTest {
    private static final Identifier OAK_LOG = Identifier.parse("minecraft:oak_log");
    private static final Identifier BIRCH_LOG = Identifier.parse("minecraft:birch_log");
    private static final Identifier WHITE_WOOL = Identifier.parse("minecraft:white_wool");
    private static final Identifier LOGS = Identifier.parse("minecraft:logs");
    private static final Identifier WOOL = Identifier.parse("minecraft:wool");
    private static final Set<Identifier> KNOWN_ITEMS = Set.of(OAK_LOG, BIRCH_LOG, WHITE_WOOL);

    private AspectRegistry registry;
    private List<String> warnings;

    @BeforeEach
    void setUp() {
        registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
        warnings = new ArrayList<>();
    }

    @Test
    void parsesItemsTagsAndEmptyAspects() {
        ItemAspectFile file = parse("""
                { "values": [
                  { "target": "minecraft:oak_log", "aspects": { "thaumory:herba": 4, "thaumory:terra": 1 } },
                  { "target": "#minecraft:wool", "aspects": { "thaumory:bestia": 2 } },
                  { "target": "minecraft:birch_log", "aspects": {} }
                ] }""");

        assertEquals(new ItemAspectFile.Target(OAK_LOG, false), file.values().get(0).target());
        assertEquals(new ItemAspectFile.Target(WOOL, true), file.values().get(1).target());
        assertEquals("#minecraft:wool", file.values().get(1).target().toString());
        assertTrue(file.values().get(2).aspects().isEmpty());
    }

    @Test
    void rejectsZeroAmount() {
        var result = ItemAspectFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                { "values": [ { "target": "minecraft:oak_log", "aspects": { "thaumory:herba": 0 } } ] }"""));
        assertTrue(result.error().isPresent());
    }

    @Test
    void itemEntryWinsOverTag() {
        ItemAspectTable table = build(parse("""
                { "values": [
                  { "target": "minecraft:oak_log", "aspects": { "thaumory:herba": 4, "thaumory:terra": 1 } },
                  { "target": "#minecraft:logs", "aspects": { "thaumory:herba": 3 } }
                ] }"""));

        Predicate<Identifier> inLogs = LOGS::equals;
        assertEquals(Optional.of(AspectList.of(new AspectStack(HERBA, 4), new AspectStack(TERRA, 1))),
                table.lookup(OAK_LOG, inLogs));
        assertEquals(Optional.of(AspectList.of(HERBA, 3)), table.lookup(BIRCH_LOG, inLogs));
    }

    @Test
    void laterFileWinsForSameItemAndOverlappingTags() {
        ItemAspectTable table = build(
                parse("""
                        { "values": [
                          { "target": "minecraft:oak_log", "aspects": { "thaumory:herba": 4 } },
                          { "target": "#minecraft:logs", "aspects": { "thaumory:herba": 3 } }
                        ] }"""),
                parse("""
                        { "values": [
                          { "target": "minecraft:oak_log", "aspects": { "thaumory:terra": 9 } },
                          { "target": "#minecraft:wool", "aspects": { "thaumory:bestia": 2 } }
                        ] }"""));

        assertEquals(Optional.of(AspectList.of(TERRA, 9)), table.lookup(OAK_LOG, tag -> false));
        // An item in both tags gets the one loaded last.
        assertEquals(Optional.of(AspectList.of(BESTIA, 2)), table.lookup(BIRCH_LOG, tag -> true));
    }

    @Test
    void emptyAspectsMeansExplicitlyNone() {
        ItemAspectTable table = build(parse("""
                { "values": [ { "target": "minecraft:white_wool", "aspects": {} } ] }"""));

        assertEquals(Optional.of(AspectList.empty()), table.lookup(WHITE_WOOL, tag -> false));
        assertEquals(Optional.empty(), table.lookup(OAK_LOG, tag -> false));
    }

    @Test
    void skipsUnknownAspectAndItemWithWarnings() {
        ItemAspectTable table = build(parse("""
                { "values": [
                  { "target": "minecraft:oak_log", "aspects": { "thaumory:nonexistent": 1 } },
                  { "target": "minecraft:missing_item", "aspects": { "thaumory:aqua": 1 } },
                  { "target": "minecraft:white_wool", "aspects": { "thaumory:aqua": 1 } }
                ] }"""));

        assertEquals(1, table.size());
        assertEquals(Optional.of(AspectList.of(AQUA, 1)), table.lookup(WHITE_WOOL, tag -> false));
        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("thaumory:nonexistent"));
        assertTrue(warnings.get(1).contains("minecraft:missing_item"));
    }

    private ItemAspectFile parse(String json) {
        return ItemAspectFile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private ItemAspectTable build(ItemAspectFile... files) {
        List<Map.Entry<Identifier, ItemAspectFile>> entries = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            entries.add(Map.entry(Identifier.parse("test:file_" + i), files[i]));
        }
        return ItemAspectTable.build(entries, registry, KNOWN_ITEMS::contains, warnings::add);
    }
}
