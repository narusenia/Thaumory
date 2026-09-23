package one.nxeu.thaumory.text;

/**
 * The unreadable glyphs an unknown aspect shows instead of its name: three or four of the
 * {@link #COUNT} glyphs in the private use area, picked from the aspect's id. The same aspect always
 * shows the same glyphs, but they spell nothing.
 */
public final class UnknownGlyphs {
    public static final int FIRST = 0xE000;
    public static final int COUNT = 16;

    private UnknownGlyphs() {}

    public static String of(String aspectId) {
        long h = 0xCBF29CE484222325L;
        for (int i = 0; i < aspectId.length(); i++) {
            h ^= aspectId.charAt(i);
            h *= 0x100000001B3L;
        }
        int length = 3 + (int) Long.remainderUnsigned(h, 2);
        StringBuilder glyphs = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            h ^= h >>> 29;
            h *= 0xBF58476D1CE4E5B9L;
            h ^= h >>> 32;
            glyphs.appendCodePoint(FIRST + (int) Long.remainderUnsigned(h, COUNT));
        }
        return glyphs.toString();
    }
}
