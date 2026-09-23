package one.nxeu.thaumory.client;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import one.nxeu.thaumory.api.flux.FluxStage;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.text.ThaumoryText;

/** How a Flux stage reads: its colour and text effect (requirements §7.6), wherever it is shown. */
public final class FluxStageText {
    private FluxStageText() {}

    public static int color(FluxStage stage) {
        return switch (stage) {
            case NONE -> 0xFFAAAAAA;
            case STAGNATION -> 0xFFD7A6FF;
            case EROSION -> 0xFFB266FF;
            case MANIFESTATION -> 0xFFE0409A;
            case OVERLOAD -> 0xFFFF4040;
        };
    }

    public static Optional<TextEffect> effect(FluxStage stage) {
        return switch (stage) {
            case NONE -> Optional.empty();
            case STAGNATION -> Optional.of(TextEffect.PULSE);
            case EROSION -> Optional.of(TextEffect.FLICKER);
            case MANIFESTATION -> Optional.of(TextEffect.WAVE);
            case OVERLOAD -> Optional.of(TextEffect.SHAKE);
        };
    }

    /** {@code key + "." + stage}, in the stage's colour and with its effect. */
    public static MutableComponent styled(String key, FluxStage stage) {
        MutableComponent text = Component.translatable(key + "." + stage.name().toLowerCase(Locale.ROOT)).withColor(color(stage));
        return effect(stage).map(effect -> ThaumoryText.withEffect(text, effect)).orElse(text);
    }
}
