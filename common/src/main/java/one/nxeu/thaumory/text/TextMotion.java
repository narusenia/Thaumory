package one.nxeu.thaumory.text;

/**
 * Where a glyph drawn with an effect sits and how bright it is at a given moment. Each glyph is told
 * apart by its x position, so neighbours move on their own while one glyph stays put between the
 * frames of a step.
 */
public final class TextMotion {
    private static final float SHAKE_AMOUNT = 0.9f;
    private static final long SHAKE_STEP_MILLIS = 40;
    private static final float TREMBLE_AMOUNT = 0.4f;
    private static final long TREMBLE_STEP_MILLIS = 90;
    private static final float WAVE_AMOUNT = 1.2f;
    private static final float SHIMMER_AMOUNT = 0.4f;
    private static final long FLICKER_STEP_MILLIS = 110;

    private TextMotion() {}

    /** The glyph's horizontal offset in GUI pixels. */
    public static float offsetX(TextEffect effect, float glyphX, long millis) {
        return switch (effect) {
            case SHAKE -> jitter(glyphX, millis / SHAKE_STEP_MILLIS, 1) * SHAKE_AMOUNT;
            case TREMBLE -> jitter(glyphX, millis / TREMBLE_STEP_MILLIS, 1) * TREMBLE_AMOUNT;
            default -> 0;
        };
    }

    /** The glyph's vertical offset in GUI pixels; positive is down. */
    public static float offsetY(TextEffect effect, float glyphX, long millis) {
        float seconds = millis / 1000f;
        return switch (effect) {
            case SHAKE -> jitter(glyphX, millis / SHAKE_STEP_MILLIS, 2) * SHAKE_AMOUNT;
            case TREMBLE -> jitter(glyphX, millis / TREMBLE_STEP_MILLIS, 2) * TREMBLE_AMOUNT;
            case WAVE -> (float) Math.sin(seconds * 5 - glyphX * 0.35f) * WAVE_AMOUNT;
            case SHIMMER -> (float) Math.sin(seconds * 2 - glyphX * 0.5f) * SHIMMER_AMOUNT;
            default -> 0;
        };
    }

    /** How much of the glyph's colour to keep, from 0 to 1. */
    public static float brightness(TextEffect effect, float glyphX, long millis) {
        float seconds = millis / 1000f;
        return switch (effect) {
            case PULSE -> 0.6f + 0.4f * (0.5f + 0.5f * (float) Math.sin(seconds * 3));
            case FLICKER -> 0.45f + 0.55f * (0.5f + 0.5f * jitter(glyphX, millis / FLICKER_STEP_MILLIS, 3));
            default -> 1;
        };
    }

    /** A repeatable number in [-1, 1] for this glyph, time step and axis. */
    static float jitter(float glyphX, long step, int axis) {
        long h = Float.floatToIntBits(glyphX) * 0x9E3779B97F4A7C15L + step * 0xC2B2AE3D27D4EB4FL + axis * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return ((h >>> 11) / (float) (1L << 53)) * 2 - 1;
    }
}
