package one.nxeu.thaumory.circle;

import net.minecraft.core.Direction;

/**
 * The four compass points of a ring, each with a node on it. North is towards negative z on the
 * floor; on other faces the sides turn with the face ({@link CirclePlane}).
 */
public enum CircleSide {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dz;

    CircleSide(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** The world direction of this side for a circle whose front is {@code front}. */
    public Direction inWorld(Direction front) {
        return CirclePlane.toWorld(front, Direction.valueOf(name()));
    }

    /** The node of ring {@code ring} on this side, as an offset from the Core ({@link CircleScan#radius}). */
    public int nodeX(int ring) {
        return dx * CircleScan.radius(ring);
    }

    public int nodeZ(int ring) {
        return dz * CircleScan.radius(ring);
    }
}
