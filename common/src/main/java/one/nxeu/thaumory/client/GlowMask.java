package one.nxeu.thaumory.client;

/**
 * The soft light around a picture: its alpha, spread out by a blur over a margin added on every
 * side so the light can reach past the edges. Plain arithmetic, so it can be tested without a game.
 */
public final class GlowMask {
    private GlowMask() {}

    /**
     * @param alpha  the picture's alpha, row by row, {@code width × height}, each 0 to 255
     * @param margin pixels added on each side
     * @param radius how far the blur reaches, in pixels
     * @return the blurred alpha, {@code (width + 2 × margin) × (height + 2 × margin)}, each 0 to 255
     */
    public static int[] of(int[] alpha, int width, int height, int margin, int radius) {
        int w = width + 2 * margin;
        int h = height + 2 * margin;
        double[] grid = new double[w * h];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[(y + margin) * w + x + margin] = alpha[y * width + x];
            }
        }
        // Two box blurs one after the other come close to a Gaussian.
        for (int pass = 0; pass < 2; pass++) {
            grid = boxBlur(grid, w, h, radius, true);
            grid = boxBlur(grid, w, h, radius, false);
        }
        int[] out = new int[w * h];
        for (int i = 0; i < out.length; i++) {
            out[i] = (int) Math.min(255, Math.round(grid[i]));
        }
        return out;
    }

    private static double[] boxBlur(double[] grid, int w, int h, int radius, boolean horizontal) {
        double[] out = new double[grid.length];
        int size = 2 * radius + 1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double sum = 0;
                for (int d = -radius; d <= radius; d++) {
                    int sx = horizontal ? x + d : x;
                    int sy = horizontal ? y : y + d;
                    if (sx >= 0 && sx < w && sy >= 0 && sy < h) {
                        sum += grid[sy * w + sx];
                    }
                }
                out[y * w + x] = sum / size;
            }
        }
        return out;
    }
}
