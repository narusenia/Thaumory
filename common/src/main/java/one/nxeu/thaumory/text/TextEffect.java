package one.nxeu.thaumory.text;

/**
 * Ways text can move or glow. Each is a font id under {@code thaumory:effect/}, drawn with the
 * default font's glyphs, so it works wherever a text component goes. The unknown-aspect glyphs
 * ({@code thaumory:glyphs}) always shimmer.
 */
public enum TextEffect {
    /** Colours drift through the glyph, as if seen through heat. Drawn by a shader. */
    SHIMMER("shimmer", true),
    /** Slowly brightens and dims. */
    PULSE("pulse", false),
    /** Each glyph flickers on its own, like ink bleeding. */
    FLICKER("flicker", false),
    /** Glyphs bob up and down in a travelling wave. */
    WAVE("wave", false),
    /** A slight, nervous tremble. */
    TREMBLE("tremble", false),
    /** A violent shake. */
    SHAKE("shake", false),
    /** A band of light sweeps across. Drawn by a shader. */
    STREAK("streak", true);

    private final String name;
    private final boolean shader;

    TextEffect(String name, boolean shader) {
        this.name = name;
        this.shader = shader;
    }

    public String id() {
        return name;
    }

    /** Whether a fragment shader draws it; the others only move glyphs or scale their colour. */
    public boolean usesShader() {
        return shader;
    }
}
