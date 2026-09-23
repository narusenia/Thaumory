package one.nxeu.thaumory.api.aspect;

/**
 * A direction on the aspect hexagon. Primal aspects are unit vectors; a compound aspect is the sum
 * of its components.
 */
public record AspectVector(double x, double y) {
    public static final AspectVector ZERO = new AspectVector(0, 0);

    public static AspectVector ofDegrees(double degrees) {
        double radians = Math.toRadians(degrees);
        return new AspectVector(Math.cos(radians), Math.sin(radians));
    }

    public AspectVector add(AspectVector other) {
        return new AspectVector(x + other.x, y + other.y);
    }

    public AspectVector scale(double factor) {
        return new AspectVector(x * factor, y * factor);
    }

    public double length() {
        return Math.hypot(x, y);
    }

    public boolean isCloseTo(AspectVector other, double epsilon) {
        return Math.abs(x - other.x) <= epsilon && Math.abs(y - other.y) <= epsilon;
    }
}
