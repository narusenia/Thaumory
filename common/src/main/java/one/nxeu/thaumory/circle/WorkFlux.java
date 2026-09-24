package one.nxeu.thaumory.circle;

/**
 * The Flux a circle gives off as it works (requirements §4.5), held back in the Core until it
 * comes to a whole unit, since a chunk forgets Flux below 1. It goes out at once when the chunk
 * already holds that much.
 */
public final class WorkFlux {
    private WorkFlux() {}

    /** @param released what goes into the chunk now; {@code held} what the Core keeps back */
    public record Step(double released, double held) {}

    public static Step add(double held, double gain, double inChunk) {
        double waiting = held + Math.max(0, gain);
        return waiting >= 1 || (inChunk >= 1 && waiting > 0) ? new Step(waiting, 0) : new Step(0, waiting);
    }
}
