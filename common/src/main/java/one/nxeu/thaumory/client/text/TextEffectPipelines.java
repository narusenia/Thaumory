package one.nxeu.thaumory.client.text;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Style;
import net.minecraft.client.gui.font.TextRenderable;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.text.ThaumoryText;

/**
 * The GUI text pipelines for effects drawn by a shader, in colour and grayscale versions to match
 * the glyph sheet they replace. Effects without a shader keep the glyph's own pipeline.
 */
public final class TextEffectPipelines {
    private static final RenderPipeline SHIMMER = build("shimmer", "SHIMMER", false);
    private static final RenderPipeline SHIMMER_GRAYSCALE = build("shimmer_grayscale", "SHIMMER", true);
    private static final RenderPipeline STREAK = build("streak", "STREAK", false);
    private static final RenderPipeline STREAK_GRAYSCALE = build("streak_grayscale", "STREAK", true);

    private TextEffectPipelines() {}

    private static RenderPipeline build(String name, String define, boolean grayscale) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.GUI_TEXT_SNIPPET)
                .withLocation(Thaumory.id("pipeline/gui_text_" + name))
                .withVertexShader(Thaumory.id("core/text_effect"))
                .withFragmentShader(Thaumory.id("core/text_effect"))
                .withShaderDefine(define);
        if (grayscale) {
            builder.withShaderDefine("IS_GRAYSCALE");
        }
        return builder.build();
    }

    static RenderPipeline forGui(TextEffect effect, RenderPipeline original) {
        boolean grayscale = original == RenderPipelines.GUI_TEXT_GRAYSCALE;
        return switch (effect) {
            case SHIMMER -> grayscale ? SHIMMER_GRAYSCALE : SHIMMER;
            case STREAK -> grayscale ? STREAK_GRAYSCALE : STREAK;
            default -> original;
        };
    }

    /** Wraps a glyph whose font is one of Thaumory's effects; leaves every other glyph alone. */
    public static TextRenderable.Styled wrap(TextRenderable.Styled glyph) {
        if (glyph == null) {
            return null;
        }
        Style style = glyph.style();
        return ThaumoryText.effectOf(style.getFont()).<TextRenderable.Styled>map(effect -> new EffectGlyph(glyph, effect)).orElse(glyph);
    }
}
