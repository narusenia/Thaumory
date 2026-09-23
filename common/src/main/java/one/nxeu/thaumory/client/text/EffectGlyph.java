package one.nxeu.thaumory.client.text;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.text.TextMotion;
import org.joml.Matrix4fc;

/**
 * A glyph drawn with a {@link TextEffect}: moved and dimmed as {@link TextMotion} says each time it
 * is drawn, and in the GUI through the effect's shader when it has one. Text in the world keeps the
 * plain render type.
 */
record EffectGlyph(TextRenderable.Styled delegate, TextEffect effect) implements TextRenderable.Styled {
    @Override
    public void render(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, boolean flat) {
        long millis = System.currentTimeMillis();
        float x = delegate.left();
        delegate.render(pose, new EffectVertexConsumer(buffer,
                TextMotion.offsetX(effect, x, millis), TextMotion.offsetY(effect, x, millis), TextMotion.brightness(effect, x, millis)),
                packedLightCoords, flat);
    }

    @Override
    public RenderType renderType(Font.DisplayMode displayMode) {
        return delegate.renderType(displayMode);
    }

    @Override
    public GpuTextureView textureView() {
        return delegate.textureView();
    }

    @Override
    public RenderPipeline guiPipeline() {
        return TextEffectPipelines.forGui(effect, delegate.guiPipeline());
    }

    @Override
    public float left() {
        return delegate.left();
    }

    @Override
    public float top() {
        return delegate.top();
    }

    @Override
    public float right() {
        return delegate.right();
    }

    @Override
    public float bottom() {
        return delegate.bottom();
    }

    @Override
    public Style style() {
        return delegate.style();
    }

    @Override
    public float activeLeft() {
        return delegate.activeLeft();
    }

    @Override
    public float activeTop() {
        return delegate.activeTop();
    }

    @Override
    public float activeRight() {
        return delegate.activeRight();
    }

    @Override
    public float activeBottom() {
        return delegate.activeBottom();
    }
}
