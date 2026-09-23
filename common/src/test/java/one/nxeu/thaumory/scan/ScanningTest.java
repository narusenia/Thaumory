package one.nxeu.thaumory.scan;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.METALLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import org.junit.jupiter.api.Test;

class ScanningTest {
    private static final Identifier OAK_LOG = Identifier.parse("minecraft:oak_log");
    private static final Identifier BIRCH_LOG = Identifier.parse("minecraft:birch_log");
    private static final Identifier SPRUCE_LOG = Identifier.parse("minecraft:spruce_log");
    private static final Identifier IRON_ORE = Identifier.parse("minecraft:iron_ore");
    private static final Identifier BARRIER = Identifier.parse("minecraft:barrier");

    private final Map<Identifier, AspectList> aspects = new HashMap<>(Map.of(
            OAK_LOG, AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 4)),
            BIRCH_LOG, AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 4)),
            SPRUCE_LOG, AspectList.of(HERBA, 16),
            IRON_ORE, AspectList.of(new AspectStack(METALLUM, 16), new AspectStack(TERRA, 16))));

    private Scanning.Result scan(PlayerKnowledge knowledge, Identifier item, int revealAfter) {
        return Scanning.scanItem(knowledge, item, id -> aspects.getOrDefault(id, AspectList.empty()), new ScanSettings(revealAfter));
    }

    @Test
    void revealsAfterEnoughDifferentItems() {
        Scanning.Result first = scan(PlayerKnowledge.EMPTY, OAK_LOG, 3);
        Scanning.Result second = scan(first.knowledge(), BIRCH_LOG, 3);
        assertEquals(List.of(), second.revealed());

        Scanning.Result third = scan(second.knowledge(), SPRUCE_LOG, 3);
        assertEquals(List.of(HERBA), third.revealed());
        assertTrue(third.knowledge().knowsAspect(HERBA.id()));
        assertFalse(third.knowledge().knowsAspect(TERRA.id()));

        // Terra has been in oak and birch logs; the ore is its third item.
        Scanning.Result fourth = scan(third.knowledge(), IRON_ORE, 3);
        assertEquals(List.of(TERRA), fourth.revealed());
    }

    @Test
    void scanningTheSameItemAgainDoesNotCount() {
        Scanning.Result first = scan(PlayerKnowledge.EMPTY, OAK_LOG, 2);
        Scanning.Result again = scan(first.knowledge(), OAK_LOG, 2);

        assertTrue(first.newlyScanned());
        assertFalse(again.newlyScanned());
        assertEquals(List.of(), again.revealed());
        assertEquals(first.knowledge(), again.knowledge());
    }

    @Test
    void revealsLargestAmountFirst() {
        Scanning.Result result = scan(PlayerKnowledge.EMPTY, OAK_LOG, 1);
        assertEquals(List.of(HERBA, TERRA), result.revealed());
    }

    @Test
    void rescanRevealsAfterThresholdIsLowered() {
        PlayerKnowledge knowledge = scan(PlayerKnowledge.EMPTY, OAK_LOG, 3).knowledge();
        knowledge = scan(knowledge, BIRCH_LOG, 3).knowledge();

        Scanning.Result rescan = scan(knowledge, OAK_LOG, 2);
        assertFalse(rescan.newlyScanned());
        assertEquals(List.of(HERBA, TERRA), rescan.revealed());
    }

    @Test
    void recordsItemsWithoutAspects() {
        Scanning.Result result = scan(PlayerKnowledge.EMPTY, BARRIER, 1);

        assertTrue(result.newlyScanned());
        assertTrue(result.knowledge().hasScanned(PlayerKnowledge.ITEMS, BARRIER));
        assertEquals(List.of(), result.revealed());
    }

    @Test
    void countsWithCurrentAspects() {
        PlayerKnowledge knowledge = scan(PlayerKnowledge.EMPTY, SPRUCE_LOG, 2).knowledge();
        // A datapack gives spruce logs Metallum after they were scanned.
        aspects.put(SPRUCE_LOG, AspectList.of(METALLUM, 4));

        assertEquals(List.of(METALLUM), scan(knowledge, IRON_ORE, 2).revealed());
    }

    @Test
    void parsesSettings() {
        assertEquals(new ScanSettings(5), ScanSettings.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{ \"reveal_after\": 5 }")).getOrThrow());
        assertTrue(ScanSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{ \"reveal_after\": 0 }")).isError());
    }
}
