package one.nxeu.thaumory.aspect;

import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.text.ThaumoryText;
import one.nxeu.thaumory.text.UnknownGlyphs;

/**
 * How aspects read in tooltips and messages: the aspect's icon, then its name, both in its color.
 * Unknown aspects show a shared icon and shimmering glyphs of their own instead, so neither the
 * picture nor the name gives anything away.
 */
public final class AspectText {
    /** Icons live in the GUI atlas: {@code assets/<namespace>/textures/gui/sprites/aspect/<path>.png}. */
    public static final Identifier UNKNOWN_ICON = Identifier.fromNamespaceAndPath("thaumory", "aspect/unknown");

    private AspectText() {}

    public static MutableComponent name(Aspect aspect, boolean known) {
        return Component.empty()
                .append(icon(aspect, known))
                .append(" ")
                .append(known ? knownName(aspect) : glyphs(aspect))
                .withColor(aspect.color());
    }

    /** The translated name, with the aspect's own effect if it has one. */
    private static MutableComponent knownName(Aspect aspect) {
        MutableComponent name = Component.translatable(aspect.translationKey());
        return aspect.nameEffect().map(effect -> ThaumoryText.withEffect(name, effect)).orElse(name);
    }

    /** A known aspect with {@code effect} on its name (not its icon). */
    public static MutableComponent name(Aspect aspect, TextEffect effect) {
        return Component.empty()
                .append(icon(aspect, true))
                .append(" ")
                .append(ThaumoryText.withEffect(Component.translatable(aspect.translationKey()), effect))
                .withColor(aspect.color());
    }

    /** What an unknown aspect shows for a name: its own glyphs, which always shimmer. */
    public static MutableComponent glyphs(Aspect aspect) {
        return Component.literal(UnknownGlyphs.of(aspect.id().toString())).withStyle(style -> style.withFont(ThaumoryText.GLYPHS));
    }

    /** The aspect's icon sprite, or the shared unknown one. */
    public static Identifier iconSprite(Aspect aspect, boolean known) {
        return known ? aspect.id().withPrefix("aspect/") : UNKNOWN_ICON;
    }

    /** The icon alone, tinted with the aspect's color. */
    public static MutableComponent icon(Aspect aspect, boolean known) {
        return Component.object(new AtlasSprite(AtlasIds.GUI, iconSprite(aspect, known))).withColor(aspect.color());
    }

    /** e.g. "Herba ×16". */
    public static MutableComponent stack(AspectStack stack, boolean known) {
        return name(stack.aspect(), known).append(Component.literal(" ×" + stack.amount()).withColor(0xAAAAAA));
    }

    /** Every aspect, largest first, separated by commas. */
    public static MutableComponent list(AspectList aspects, Predicate<Aspect> known) {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (AspectStack stack : aspects.sortedByAmount()) {
            if (!first) {
                text.append(", ");
            }
            text.append(stack(stack, known.test(stack.aspect())));
            first = false;
        }
        return text;
    }
}
