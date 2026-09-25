package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
 * <p>Another Core on a node is part of the ring too, with no modifier there: it is a seat for a
 * sub-circle ({@link CircleChildren}). A Core anywhere else on a ring does not hold it.
 *
 * @param rings            rings that hold, 0 to the limit the scan was given
 * @param nodes            modifiers on the nodes of those rings, inner ring first
 * @param ignoredModifiers modifiers on those rings but off their nodes
 * @param seats            Cores on the nodes of those rings, inner ring first
 */
public record CircleScan(int rings, List<Node> nodes, List<Offset> ignoredModifiers, List<Node> seats) {
    /** Nothing holds. */
    public static final CircleScan NONE = new CircleScan(0, List.of(), List.of(), List.of());

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

    /** A scan where no cell holds a Core. */
    public static CircleScan scan(Patterns patterns, Identifier line, int maxRings) {
        return scan(patterns, line, id -> false, maxRings);
    }

    /**
     * @param line     the plain line's id; every other pattern is a modifier
     * @param cores    which of the ids {@code patterns} gives are Cores
     * @param maxRings how many rings the Core reads, at most {@link #MAX_RINGS}
     */
    public static CircleScan scan(Patterns patterns, Identifier line, Predicate<Identifier> cores, int maxRings) {
        int rings = 0;
        List<Node> nodes = new ArrayList<>();
        List<Offset> ignored = new ArrayList<>();
        List<Node> seats = new ArrayList<>();
        for (int ring = 1; ring <= Math.min(maxRings, MAX_RINGS); ring++) {
            Ring read = new Ring();
            if (!readRing(patterns, line, cores, ring, read)) {
                break;
            }
            rings = ring;
            nodes.addAll(read.nodes);
            ignored.addAll(read.ignored);
            seats.addAll(read.seats);
        }
        return new CircleScan(rings, List.copyOf(nodes), List.copyOf(ignored), List.copyOf(seats));
    }

    /** The seats on ring {@code ring}, the outermost that holds when it is {@link #rings}. */
    public List<Node> seatsOn(int ring) {
        return seats.stream().filter(seat -> seat.ring() == ring).toList();
    }

    private static final class Ring {
        final List<Node> nodes = new ArrayList<>();
        final List<Offset> ignored = new ArrayList<>();
        final List<Node> seats = new ArrayList<>();
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

    private static boolean readRing(Patterns patterns, Identifier line, Predicate<Identifier> cores, int ring, Ring read) {
        for (Offset cell : cells(ring)) {
            Optional<Identifier> pattern = patterns.at(cell.dx(), cell.dz());
            if (pattern.isEmpty()) {
                return false;
            }
            if (pattern.get().equals(line)) {
                continue;
            }
            Optional<CircleSide> side = nodeSide(ring, cell.dx(), cell.dz());
            if (cores.test(pattern.get())) {
                if (side.isEmpty()) {
                    return false;
                }
                read.seats.add(new Node(ring, side.get(), pattern.get()));
            } else if (side.isPresent()) {
                read.nodes.add(new Node(ring, side.get(), pattern.get()));
            } else {
                read.ignored.add(cell);
            }
        }
        read.nodes.sort((a, b) -> a.side().compareTo(b.side()));
        read.seats.sort((a, b) -> a.side().compareTo(b.side()));
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
