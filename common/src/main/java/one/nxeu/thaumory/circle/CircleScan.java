package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Reads the chalk around a Core: how many rings hold and which modifiers sit on their nodes.
 *
 * <p>Ring {@code r} is every cell at Chebyshev distance {@code r} from the Core, all of which must
 * hold a pattern. The first ring that does not hold stops the count, so rings beyond it do not
 * count either. A node is the middle cell of a side; a modifier anywhere else on a ring works as
 * a line and is reported as ignored.
 *
 * @param rings            rings that hold, 0 to {@link #MAX_RINGS}
 * @param nodes            modifiers on the nodes of those rings, inner ring first
 * @param ignoredModifiers modifiers on those rings but off their nodes
 */
public record CircleScan(int rings, List<Node> nodes, List<Offset> ignoredModifiers) {
    public static final int MAX_RINGS = 3;

    public record Node(int ring, CircleSide side, Identifier pattern) {}

    /** A cell relative to the Core. */
    public record Offset(int dx, int dz) {}

    /** The pattern drawn in a cell relative to the Core, if any. */
    @FunctionalInterface
    public interface Patterns {
        Optional<Identifier> at(int dx, int dz);
    }

    /** @param line the plain line's id; every other pattern is a modifier */
    public static CircleScan scan(Patterns patterns, Identifier line) {
        int rings = 0;
        List<Node> nodes = new ArrayList<>();
        List<Offset> ignored = new ArrayList<>();
        for (int ring = 1; ring <= MAX_RINGS; ring++) {
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

    private static boolean readRing(Patterns patterns, Identifier line, int ring, List<Node> nodes, List<Offset> ignored) {
        for (int dz = -ring; dz <= ring; dz++) {
            for (int dx = -ring; dx <= ring; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                    continue;
                }
                Optional<Identifier> pattern = patterns.at(dx, dz);
                if (pattern.isEmpty()) {
                    return false;
                }
                if (pattern.get().equals(line)) {
                    continue;
                }
                Optional<CircleSide> side = nodeSide(ring, dx, dz);
                if (side.isPresent()) {
                    nodes.add(new Node(ring, side.get(), pattern.get()));
                } else {
                    ignored.add(new Offset(dx, dz));
                }
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
