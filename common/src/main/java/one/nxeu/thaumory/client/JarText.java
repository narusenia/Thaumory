package one.nxeu.thaumory.client;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.network.chat.Component;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** A jar's label and contents as lines, for its tooltip and the loupe HUD. */
final class JarText {
    private static final int GRAY = 0xFFAAAAAA;

    private JarText() {}

    /** @param capacity shown after the total when known (the item tooltip cannot know the server's) */
    static List<Component> describe(JarContents contents, OptionalInt capacity, PlayerKnowledge knowledge) {
        List<Component> lines = new ArrayList<>();
        contents.label().ifPresent(label -> lines.add(Component.translatable("tooltip.thaumory.jar.label",
                AspectText.name(label, knowledge.knowsAspect(label.id()))).withColor(GRAY)));
        int total = contents.aspects().total();
        lines.add((capacity.isPresent()
                ? Component.translatable("hud.thaumory.crucible.essentia", total, capacity.getAsInt())
                : Component.translatable("tooltip.thaumory.jar.essentia", total)).withColor(GRAY));
        for (AspectStack stack : contents.aspects().sortedByAmount()) {
            lines.add(Component.literal(" ").append(AspectText.stack(stack, knowledge.knowsAspect(stack.aspect().id()))));
        }
        return lines;
    }
}
