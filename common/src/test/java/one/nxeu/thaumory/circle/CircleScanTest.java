package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class CircleScanTest {
    private static final Identifier LINE = Identifier.fromNamespaceAndPath("thaumory", "chalk_line");
    private static final Identifier AMPLIFY = Identifier.fromNamespaceAndPath("thaumory", "amplifying_pattern");
    private static final Identifier STABILIZE = Identifier.fromNamespaceAndPath("thaumory", "stabilizing_pattern");
    private static final Identifier EXTEND = Identifier.fromNamespaceAndPath("thaumory", "extending_pattern");

    /** Chalk around a Core at (0, 0). */
    private static final class Ground {
        private final Map<CircleScan.Offset, Identifier> cells = new HashMap<>();

        Ground ring(int ring) {
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        cells.put(new CircleScan.Offset(dx, dz), LINE);
                    }
                }
            }
            return this;
        }

        Ground node(int ring, CircleSide side, Identifier pattern) {
            return set(side.nodeX(ring), side.nodeZ(ring), pattern);
        }

        Ground set(int dx, int dz, Identifier pattern) {
            cells.put(new CircleScan.Offset(dx, dz), pattern);
            return this;
        }

        Ground erase(int dx, int dz) {
            cells.remove(new CircleScan.Offset(dx, dz));
            return this;
        }

        CircleScan scan() {
            return CircleScan.scan((dx, dz) -> Optional.ofNullable(cells.get(new CircleScan.Offset(dx, dz))), LINE);
        }
    }

    @Test
    void nothingDrawnHoldsNoRings() {
        CircleScan scan = new Ground().scan();
        assertEquals(0, scan.rings());
        assertTrue(scan.nodes().isEmpty());
    }

    @Test
    void completeRingsCount() {
        assertEquals(1, new Ground().ring(1).scan().rings());
        assertEquals(3, new Ground().ring(1).ring(2).ring(3).scan().rings());
    }

    @Test
    void ringWithAGapDoesNotHold() {
        assertEquals(0, new Ground().ring(1).erase(1, 1).scan().rings());
    }

    @Test
    void brokenRingDisablesTheRingsOutsideIt() {
        CircleScan scan = new Ground().ring(1).ring(2).ring(3).erase(-2, 1)
                .node(3, CircleSide.EAST, AMPLIFY).scan();

        assertEquals(1, scan.rings());
        assertTrue(scan.nodes().isEmpty(), "the modifier on ring 3 must not count");
    }

    @Test
    void twelveNodesOnThreeRings() {
        Ground ground = new Ground().ring(1).ring(2).ring(3);
        for (int ring = 1; ring <= 3; ring++) {
            for (CircleSide side : CircleSide.values()) {
                ground.node(ring, side, AMPLIFY);
            }
        }
        CircleScan scan = ground.scan();

        assertEquals(3, scan.rings());
        assertEquals(12, scan.nodes().size());
        assertEquals(new CircleScan.Node(1, CircleSide.NORTH, AMPLIFY), scan.nodes().getFirst());
        assertEquals(new CircleScan.Node(3, CircleSide.WEST, AMPLIFY), scan.nodes().getLast());
    }

    @Test
    void modifierOnANodeKeepsTheRingWhole() {
        CircleScan scan = new Ground().ring(1).node(1, CircleSide.SOUTH, STABILIZE).scan();

        assertEquals(1, scan.rings());
        assertEquals(List.of(new CircleScan.Node(1, CircleSide.SOUTH, STABILIZE)), scan.nodes());
    }

    @Test
    void modifierOffANodeWorksAsALineAndIsIgnored() {
        CircleScan scan = new Ground().ring(1).ring(2).set(2, 2, AMPLIFY).set(1, -2, EXTEND).scan();

        assertEquals(2, scan.rings());
        assertTrue(scan.nodes().isEmpty());
        assertEquals(List.of(new CircleScan.Offset(1, -2), new CircleScan.Offset(2, 2)), scan.ignoredModifiers());
    }

    @Test
    void chalkInsideTheRingsDoesNotMatter() {
        // Ring 2 is scanned on its own cells only; the Core's own cell is never read.
        assertEquals(2, new Ground().ring(1).ring(2).set(0, 0, AMPLIFY).scan().rings());
    }

    @Test
    void instabilityAddsUpNodePatternsAndNeverGoesNegative() {
        CircleSettings settings = CircleSettings.DEFAULT;
        List<CircleScan.Node> twoAmplify = List.of(
                new CircleScan.Node(1, CircleSide.NORTH, AMPLIFY), new CircleScan.Node(1, CircleSide.SOUTH, AMPLIFY));

        assertEquals(4, settings.instability(twoAmplify));
        assertEquals(1, settings.instability(List.of(twoAmplify.get(0), twoAmplify.get(1),
                new CircleScan.Node(1, CircleSide.EAST, STABILIZE))));
        assertEquals(0, settings.instability(List.of(new CircleScan.Node(1, CircleSide.EAST, STABILIZE))));
        assertEquals(0, settings.instability(List.of(new CircleScan.Node(1, CircleSide.EAST, EXTEND))), "unlisted patterns add 0");
    }

    @Test
    void settingsReadFromJson() {
        CircleSettings settings = CircleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"scan_interval": 20, "instability_threshold": 5,
                 "patterns": {"thaumory:extending_pattern": {"instability": 1}, "thaumory:amplifying_pattern": {}}}
                """)).getOrThrow();

        assertEquals(20, settings.scanInterval());
        assertEquals(5, settings.instabilityThreshold());
        assertEquals(1, settings.instability(List.of(new CircleScan.Node(1, CircleSide.NORTH, EXTEND),
                new CircleScan.Node(1, CircleSide.EAST, AMPLIFY))));
        assertTrue(CircleSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"scan_interval\": 0, \"instability_threshold\": 3}")).isError());
    }
}
