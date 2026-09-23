package one.nxeu.thaumory.aspect;

import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;

/** How aspects read in tooltips and messages. Unknown aspects show as "?" in their own color. */
public final class AspectText {
    public static final String UNKNOWN_KEY = "aspect.thaumory.unknown";

    private AspectText() {}

    public static MutableComponent name(Aspect aspect, boolean known) {
        return (known ? Component.translatable(aspect.translationKey()) : Component.translatable(UNKNOWN_KEY))
                .withColor(aspect.color());
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
