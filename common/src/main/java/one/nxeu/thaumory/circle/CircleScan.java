package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.resources.Identifier;

/**
 * Reads the chalk around a Core: how many rings hold and which modifiers sit on their nodes.
 *
 * <p>Ring {@code n} is a circle of radius {@link #radius 2n − 1} around the Core: every cell whose
 * centre lies less than half a cell from it ({@link #onRing}), all of which must hold a pattern.
 * The first ring that does not hold stops the count, so rings beyond it do not count either.
 * Cells off the rings are not read at all. A node is the cell due north, east, south or west of
 * the Core on a ring; a modifier anywhere else on a ring works as a line and is reported as
 * ignored.
 *
 * @param rings            rings that hold, 0 to the limit the scan was given
 * @param nodes            modifiers on the nodes of those rings, inner ring first
 * @param ignoredModifiers modifiers on those rings but off their nodes
 */
public record CircleScan(int rings, List<Node> nodes, List<Offset> ignoredModifiers) {
    /** The most rings any Core reads (rank 3). */
    public static final int MAX_RINGS = 5;

    private static final List<List<Offset>> CELLS = IntStream.rangeClosed(1, MAX_RINGS)
            .mapToObj(CircleScan::findCells).toList();

    public record Node(int ring, CircleSide side, Identifier pattern) {}

    /** A cell relative to the Core. */
    public record Offset(int dx, int dz) {}

    /** The pattern drawn in a cell relative to the Core, if any. */
    @FunctionalInterface
    public interface Patterns {
        Optional<Identifier> at(int dx, int dz);
    }

    /**
     * @param line     the plain line's id; every other pattern is a modifier
     * @param maxRings how many rings the Core reads, at most {@link #MAX_RINGS}
     */
    public static CircleScan scan(Patterns patterns, Identifier line, int maxRings) {
        int rings = 0;
        List<Node> nodes = new ArrayList<>();
        List<Offset> ignored = new ArrayList<>();
        for (int ring = 1; ring <= Math.min(maxRings, MAX_RINGS); ring++) {
            List<Node> ringNodes = new ArrayList<>();
            List<Offset> ringIgnored = new ArrayList<>();
            if (!readRing(patterns, line, ring, ringNodes, ringIgnored)) {
                break;
            }
            rings = ring;
            nodes.addAll(ringNodes);
            ignored.addAll(ringIgnored);
        }
        return new CircleScan(rings, List.copyOf(nodes), List.copyOf(ignored));
    }

    /** The radius of ring {@code ring}: 1, 3, 5, 7, 9. */
    public static int radius(int ring) {
        return 2 * ring - 1;
    }

    /** Whether the cell ({@code dx}, {@code dz}) from the Core lies on ring {@code ring}. */
    public static boolean onRing(int ring, int dx, int dz) {
        int r = radius(ring);
        int distance = 4 * (dx * dx + dz * dz);
        return (2 * r - 1) * (2 * r - 1) < distance && distance < (2 * r + 1) * (2 * r + 1);
    }

    /** The cells of ring {@code ring} (1 to {@link #MAX_RINGS}), row by row from the north. */
    public static List<Offset> cells(int ring) {
        if (ring < 1 || ring > MAX_RINGS) {
            throw new IllegalArgumentException("No ring " + ring);
        }
        return CELLS.get(ring - 1);
    }

    private static List<Offset> findCells(int ring) {
        int r = radius(ring);
        List<Offset> cells = new ArrayList<>();
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                if (onRing(ring, dx, dz)) {
                    cells.add(new Offset(dx, dz));
                }
            }
        }
        return List.copyOf(cells);
    }

    private static boolean readRing(Patterns patterns, Identifier line, int ring, List<Node> nodes, List<Offset> ignored) {
        for (Offset cell : cells(ring)) {
            Optional<Identifier> pattern = patterns.at(cell.dx(), cell.dz());
            if (pattern.isEmpty()) {
                return false;
            }
            if (pattern.get().equals(line)) {
                continue;
            }
            Optional<CircleSide> side = nodeSide(ring, cell.dx(), cell.dz());
            if (side.isPresent()) {
                nodes.add(new Node(ring, side.get(), pattern.get()));
            } else {
                ignored.add(cell);
            }
        }
        nodes.sort((a, b) -> a.side().compareTo(b.side()));
        return true;
    }

    private static Optional<CircleSide> nodeSide(int ring, int dx, int dz) {
        for (CircleSide side : CircleSide.values()) {
            if (side.nodeX(ring) == dx && side.nodeZ(ring) == dz) {
                return Optional.of(side);
            }
        }
        return Optional.empty();
    }
}
