package one.nxeu.thaumory.client.text;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;
import org.joml.Matrix4fc;

/** Passes a glyph's vertices on, moved by an offset and with the colour scaled. */
final class EffectVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float dx;
    private final float dy;
    private final float brightness;

    EffectVertexConsumer(VertexConsumer delegate, float dx, float dy, float brightness) {
        this.delegate = delegate;
        this.dx = dx;
        this.dy = dy;
        this.brightness = brightness;
    }

    @Override
    public VertexConsumer addVertex(Matrix4fc pose, float x, float y, float z) {
        delegate.addVertex(pose, x + dx, y + dy, z);
        return this;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x + dx, y + dy, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(scale(r), scale(g), scale(b), a);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        delegate.setColor(ARGB.color(ARGB.alpha(color), scale(ARGB.red(color)), scale(ARGB.green(color)), scale(ARGB.blue(color))));
        return this;
    }

    private int scale(int channel) {
        return Math.round(channel * brightness);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
        delegate.setUv3(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }
}
