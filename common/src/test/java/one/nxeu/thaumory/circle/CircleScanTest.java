package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            for (CircleScan.Offset cell : CircleScan.cells(ring)) {
                cells.put(cell, LINE);
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
            return scan(3);
        }

        CircleScan scan(int maxRings) {
            return CircleScan.scan((dx, dz) -> Optional.ofNullable(cells.get(new CircleScan.Offset(dx, dz))), LINE, maxRings);
        }
    }

    private static Set<CircleScan.Offset> offsets(int... xz) {
        Set<CircleScan.Offset> offsets = new HashSet<>();
        for (int i = 0; i < xz.length; i += 2) {
            offsets.add(new CircleScan.Offset(xz[i], xz[i + 1]));
        }
        return offsets;
    }

    @Test
    void theFirstRingIsTheOutlineOfTheThreeByThree() {
        assertEquals(offsets(-1, -1, 0, -1, 1, -1, -1, 0, 1, 0, -1, 1, 0, 1, 1, 1), new HashSet<>(CircleScan.cells(1)));
        assertEquals(8, CircleScan.cells(1).size());
    }

    @Test
    void theSecondRingIsACircleOfRadiusThree() {
        Set<CircleScan.Offset> expected = new HashSet<>();
        for (int[] cell : new int[][] {{3, 0}, {3, 1}, {2, 2}, {1, 3}}) {
            // Every quarter turn and mirror of the cells in one eighth.
            for (int sx : new int[] {-1, 1}) {
                for (int sz : new int[] {-1, 1}) {
                    expected.add(new CircleScan.Offset(sx * cell[0], sz * cell[1]));
                    expected.add(new CircleScan.Offset(sz * cell[1], sx * cell[0]));
                }
            }
        }
        assertEquals(16, expected.size());
        assertEquals(expected, new HashSet<>(CircleScan.cells(2)));
        assertTrue(CircleScan.onRing(2, 2, 2));
        assertTrue(CircleScan.onRing(2, 1, 3));
        assertFalse(CircleScan.onRing(2, 3, 2));
        assertFalse(CircleScan.onRing(2, 2, 1));
        assertFalse(CircleScan.onRing(2, 3, 3));
    }

    @Test
    void ringsAreRoundAndApart() {
        for (int ring = 1; ring <= CircleScan.MAX_RINGS; ring++) {
            int radius = CircleScan.radius(ring);
            assertEquals(2 * ring - 1, radius);
            for (CircleScan.Offset cell : CircleScan.cells(ring)) {
                assertTrue(Math.abs(Math.hypot(cell.dx(), cell.dz()) - radius) < 0.5, cell + " on ring " + ring);
                for (int other = 1; other <= CircleScan.MAX_RINGS; other++) {
                    assertTrue(other == ring || !CircleScan.onRing(other, cell.dx(), cell.dz()), cell + " on two rings");
                }
            }
            for (CircleSide side : CircleSide.values()) {
                assertTrue(CircleScan.cells(ring).contains(new CircleScan.Offset(side.nodeX(ring), side.nodeZ(ring))));
            }
        }
    }

    @Test
    void nodesSitOnTheCompassPoints() {
        assertEquals(0, CircleSide.NORTH.nodeX(2));
        assertEquals(-3, CircleSide.NORTH.nodeZ(2));
        assertEquals(5, CircleSide.EAST.nodeX(3));
        assertEquals(9, CircleSide.SOUTH.nodeZ(5));
        assertEquals(-7, CircleSide.WEST.nodeX(4));

        CircleScan scan = new Ground().ring(1).ring(2).node(2, CircleSide.WEST, AMPLIFY).scan();
        assertEquals(List.of(new CircleScan.Node(2, CircleSide.WEST, AMPLIFY)), scan.nodes());
    }

    @Test
    void theCountStopsAtTheFirstRingThatIsNotWhole() {
        Ground ground = new Ground().ring(1).ring(2).ring(3).ring(4).erase(2, -2);

        assertEquals(1, ground.scan(CircleScan.MAX_RINGS).rings());
        assertEquals(0, new Ground().ring(2).ring(3).scan().rings(), "without ring 1 nothing holds");
    }

    @Test
    void chalkBetweenAndOutsideTheRingsDoesNotMatter() {
        Ground ground = new Ground().ring(1).ring(2);
        CircleScan bare = ground.scan();
        // The cells between ring 1 and ring 2, a modifier there, and chalk out past ring 2.
        ground.set(2, 0, LINE).set(2, 1, AMPLIFY).set(-1, 2, EXTEND).set(3, 2, AMPLIFY).set(0, 4, LINE);

        assertEquals(2, ground.scan().rings());
        assertEquals(bare, ground.scan(), "modifiers off the rings are not counted at all");
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
    void fiveRingsHoldForACoreThatReadsFive() {
        Ground ground = new Ground().ring(1).ring(2).ring(3).ring(4).ring(5).node(5, CircleSide.EAST, AMPLIFY);
        CircleScan scan = ground.scan(CircleScan.MAX_RINGS);

        assertEquals(5, scan.rings());
        assertEquals(List.of(new CircleScan.Node(5, CircleSide.EAST, AMPLIFY)), scan.nodes());
    }

    @Test
    void theScanStopsAtTheCoresLimit() {
        Ground ground = new Ground().ring(1).ring(2).ring(3).ring(4).ring(5).node(4, CircleSide.NORTH, AMPLIFY);

        assertEquals(3, ground.scan(3).rings());
        assertTrue(ground.scan(3).nodes().isEmpty(), "the modifier on ring 4 must not count");
        assertEquals(4, ground.scan(4).rings());
        assertEquals(5, ground.scan(9).rings(), "no Core reads more than the maximum");
    }

    @Test
    void ringWithAGapDoesNotHold() {
        assertEquals(0, new Ground().ring(1).erase(1, 1).scan().rings());
    }

    @Test
    void brokenRingDisablesTheRingsOutsideIt() {
        CircleScan scan = new Ground().ring(1).ring(2).ring(3).erase(-2, 2)
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
        CircleScan scan = new Ground().ring(1).ring(2).set(2, 2, AMPLIFY).set(1, -3, EXTEND).scan();

        assertEquals(2, scan.rings());
        assertTrue(scan.nodes().isEmpty());
        assertEquals(List.of(new CircleScan.Offset(1, -3), new CircleScan.Offset(2, 2)), scan.ignoredModifiers());
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
