package one.nxeu.thaumory.circle;

/**
 * The Flux a containment circle has drawn in and not yet sealed (requirements §17.5). Each time it
 * comes to a crystal's worth, one crystal is made and that much leaves the pool.
 */
public final class CrystalPool {
    private CrystalPool() {}

    /** @param crystals how many crystals to make now; {@code held} what stays in the Core */
    public record Step(int crystals, double held) {}

    public static Step add(double held, double drawn, double perCrystal) {
        double pool = held + Math.max(0, drawn);
        if (perCrystal <= 0) {
            return new Step(0, pool);
        }
        int crystals = (int) Math.floor(pool / perCrystal);
        return new Step(crystals, pool - crystals * perCrystal);
    }
}
