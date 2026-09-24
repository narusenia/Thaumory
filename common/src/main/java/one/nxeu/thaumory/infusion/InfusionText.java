package one.nxeu.thaumory.infusion;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** An infusion as text: the circle effect's name and its level in Roman numerals. */
public final class InfusionText {
    private static final int COLOR = 0xCC99FF;

    private InfusionText() {}

    public static MutableComponent describe(Infusion infusion) {
        return Component.translatable("infusion.thaumory.entry",
                Component.translatable(infusion.effect().toLanguageKey("circle_effect")),
                Component.translatable("enchantment.level." + infusion.level())).withColor(COLOR);
    }
}
