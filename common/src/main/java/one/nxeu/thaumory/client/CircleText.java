package one.nxeu.thaumory.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** Circle combinations as the player sees them: aspects not yet worked out show as glyphs. */
public final class CircleText {
    private static final int SEPARATOR = 0xFF7A6650;

    private CircleText() {}

    /** {@code first + second / parameter / slot 4}, the separators in {@code separatorColor}. */
    public static MutableComponent combination(CircleCombination combination, PlayerKnowledge knowledge, int separatorColor) {
        MutableComponent line = aspectName(combination.first(), knowledge)
                .append(Component.literal(" + ").withColor(separatorColor))
                .append(aspectName(combination.second(), knowledge));
        combination.parameter().ifPresent(parameter -> line
                .append(Component.literal(" / ").withColor(separatorColor))
                .append(aspectName(parameter, knowledge)));
        combination.slot4().ifPresent(slot4 -> line
                .append(Component.literal(" / ").withColor(separatorColor))
                .append(aspectName(slot4, knowledge)));
        return line;
    }

    public static MutableComponent combination(CircleCombination combination, PlayerKnowledge knowledge) {
        return combination(combination, knowledge, SEPARATOR);
    }

    /** An aspect no longer registered shows its id. */
    public static MutableComponent aspectName(Identifier id, PlayerKnowledge knowledge) {
        return ThaumoryApi.aspects().get(id)
                .map(aspect -> AspectText.name(aspect, knowledge.knowsAspect(id)))
                .orElseGet(() -> Component.literal(id.toString()));
    }
}
