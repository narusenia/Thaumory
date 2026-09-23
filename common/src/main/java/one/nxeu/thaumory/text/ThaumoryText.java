package one.nxeu.thaumory.text;

import java.util.Optional;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.text.TextEffect;

/**
 * Puts {@link TextEffect}s on text components. The effect travels as the component's font, so it
 * survives being sent to clients and shows the same in chat, tooltips, HUDs and books.
 */
public final class ThaumoryText {
    /** The unreadable glyphs of unknown aspects; they always shimmer. */
    public static final FontDescription GLYPHS = new FontDescription.Resource(Thaumory.id("glyphs"));
    private static final String EFFECT_PREFIX = "effect/";

    private ThaumoryText() {}

    public static FontDescription font(TextEffect effect) {
        return new FontDescription.Resource(Thaumory.id(EFFECT_PREFIX + effect.id()));
    }

    public static MutableComponent withEffect(MutableComponent text, TextEffect effect) {
        return text.withStyle(style -> style.withFont(font(effect)));
    }

    /** The effect a font draws with, if it is one of Thaumory's. */
    public static Optional<TextEffect> effectOf(FontDescription font) {
        if (!(font instanceof FontDescription.Resource(Identifier id)) || !id.getNamespace().equals(Thaumory.MOD_ID)) {
            return Optional.empty();
        }
        if (font.equals(GLYPHS)) {
            return Optional.of(TextEffect.SHIMMER);
        }
        if (!id.getPath().startsWith(EFFECT_PREFIX)) {
            return Optional.empty();
        }
        String name = id.getPath().substring(EFFECT_PREFIX.length());
        for (TextEffect effect : TextEffect.values()) {
            if (effect.id().equals(name)) {
                return Optional.of(effect);
            }
        }
        return Optional.empty();
    }
}
