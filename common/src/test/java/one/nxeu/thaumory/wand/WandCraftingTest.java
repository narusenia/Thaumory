package one.nxeu.thaumory.wand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.wand.WandCrafting.Role;
import org.junit.jupiter.api.Test;

class WandCraftingTest {
    private static final Identifier WAND = Identifier.fromNamespaceAndPath("thaumory", "wand");
    private static final Identifier GOLD = Identifier.fromNamespaceAndPath("thaumory", "gold_wand_cap");
    private static final Identifier IRON = Identifier.fromNamespaceAndPath("thaumory", "arcane_iron_wand_cap");
    private static final Identifier STICK = Identifier.withDefaultNamespace("stick");
    private static final Identifier CRYSTAL = Identifier.fromNamespaceAndPath("thaumory", "crystal_wand_core");
    private static final Identifier DIRT = Identifier.withDefaultNamespace("dirt");
    private static final Map<Identifier, Role> ROLES = Map.of(WAND, Role.WAND, GOLD, Role.CAP, IRON, Role.CAP, STICK, Role.CORE, CRYSTAL, Role.CORE);
    private static final Function<Identifier, Role> ROLE = id -> ROLES.getOrDefault(id, Role.OTHER);

    /** A 3 × 3 grid with {@code items} at the given cells, as {cell, item, cell, item, ...}. */
    private static List<Optional<Identifier>> grid(Object... cellsAndItems) {
        List<Optional<Identifier>> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            cells.add(Optional.empty());
        }
        for (int i = 0; i < cellsAndItems.length; i += 2) {
            cells.set((Integer) cellsAndItems[i], Optional.of((Identifier) cellsAndItems[i + 1]));
        }
        return cells;
    }

    @Test
    void twoCapsAndACoreInADiagonalMakeAWand() {
        assertEquals(Optional.of(new WandCrafting.Assembly(GOLD, STICK)), WandCrafting.assembly(3, 3, grid(2, GOLD, 4, STICK, 6, GOLD), ROLE));
        assertEquals(Optional.of(new WandCrafting.Assembly(IRON, CRYSTAL)), WandCrafting.assembly(3, 3, grid(0, IRON, 4, CRYSTAL, 8, IRON), ROLE));
    }

    @Test
    void anythingElseMakesNoWand() {
        assertTrue(WandCrafting.assembly(3, 3, grid(2, GOLD, 4, STICK, 6, IRON), ROLE).isEmpty(), "mixed caps");
        assertTrue(WandCrafting.assembly(3, 3, grid(2, STICK, 4, GOLD, 6, STICK), ROLE).isEmpty(), "caps and core swapped");
        assertTrue(WandCrafting.assembly(3, 3, grid(1, GOLD, 4, STICK, 7, GOLD), ROLE).isEmpty(), "a straight line");
        assertTrue(WandCrafting.assembly(3, 3, grid(2, GOLD, 4, STICK, 6, GOLD, 0, DIRT), ROLE).isEmpty(), "something more");
        assertTrue(WandCrafting.assembly(2, 2, List.of(Optional.of(GOLD), Optional.empty(), Optional.empty(), Optional.of(STICK)), ROLE).isEmpty(),
                "too small");
    }

    @Test
    void aWandTakesTwoSameCapsOrOneCore() {
        assertEquals(Optional.of(new WandCrafting.Rebuild(0, Role.CAP, IRON, List.of(3, 5))),
                WandCrafting.rebuild(grid(0, WAND, 3, IRON, 5, IRON), ROLE));
        assertEquals(Optional.of(new WandCrafting.Rebuild(4, Role.CORE, CRYSTAL, List.of(1))), WandCrafting.rebuild(grid(1, CRYSTAL, 4, WAND), ROLE));
    }

    @Test
    void aWrongRebuildIsNone() {
        assertTrue(WandCrafting.rebuild(grid(0, WAND, 3, IRON), ROLE).isEmpty(), "one cap");
        assertTrue(WandCrafting.rebuild(grid(0, WAND, 3, IRON, 5, GOLD), ROLE).isEmpty(), "two different caps");
        assertTrue(WandCrafting.rebuild(grid(0, WAND, 3, CRYSTAL, 5, CRYSTAL), ROLE).isEmpty(), "two cores");
        assertTrue(WandCrafting.rebuild(grid(0, WAND, 3, IRON, 5, IRON, 7, CRYSTAL), ROLE).isEmpty(), "caps and a core");
        assertTrue(WandCrafting.rebuild(grid(0, WAND, 1, WAND, 3, CRYSTAL), ROLE).isEmpty(), "two wands");
        assertTrue(WandCrafting.rebuild(grid(3, IRON, 5, IRON), ROLE).isEmpty(), "no wand");
        assertTrue(WandCrafting.rebuild(grid(0, WAND, 3, CRYSTAL, 5, DIRT), ROLE).isEmpty(), "something else");
    }

    @Test
    void aPartIsACapOrACoreFromJson() {
        WandPart cap = WandPart.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"item": "thaumory:gold_wand_cap", "cap": {"essentia": 16}}""")).getOrThrow();
        assertEquals(Optional.of(new WandPart.Cap(16)), cap.cap());
        WandPart core = WandPart.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"item": "minecraft:stick", "core": {"power": 1.0, "incorrect_for": "minecraft:incorrect_for_stone_tool"}}""")).getOrThrow();
        assertEquals(Optional.of(new WandPart.Core(1.0, Identifier.withDefaultNamespace("incorrect_for_stone_tool"))), core.core());
        assertTrue(WandPart.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"item\": \"minecraft:stick\"}")).isError(), "neither");
        assertTrue(WandPart.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"item": "minecraft:stick", "cap": {"essentia": 1}, "core": {"power": 1, "incorrect_for": "minecraft:x"}}""")).isError(), "both");
    }

    @Test
    void aLaterPartForTheSameItemWins() {
        Map<Identifier, WandPart> table = WandParts.table(List.of(new WandPart(GOLD, Optional.of(new WandPart.Cap(16)), Optional.empty()),
                new WandPart(GOLD, Optional.of(new WandPart.Cap(20)), Optional.empty())));
        assertEquals(Optional.of(new WandPart.Cap(20)), WandParts.cap(table, GOLD));
        assertTrue(WandParts.core(table, GOLD).isEmpty());
    }
}
