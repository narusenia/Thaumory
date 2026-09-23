package one.nxeu.thaumory.circle;

/** The four sides of a ring, each with a node in its middle. North is towards negative z. */
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

    /** The node of ring {@code ring} on this side, as an offset from the Core. */
    public int nodeX(int ring) {
        return dx * ring;
    }

    public int nodeZ(int ring) {
        return dz * ring;
    }
}
