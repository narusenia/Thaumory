package one.nxeu.thaumory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.aspect.AspectColors;
import one.nxeu.thaumory.block.jar.JarBlockEntity;

/** Draws the Essentia in a jar as liquid: its height follows how full the jar is, its color the mix of aspects. */
final class JarRenderer implements BlockEntityRenderer<JarBlockEntity, JarRenderer.State> {
    private static final RenderType LIQUID = RenderTypes.entityTranslucent(Thaumory.id("textures/block/jar_liquid.png"));
    // Just inside the glass (3..13 wide, 12 tall), in block units.
    private static final float MIN = 3.5f / 16;
    private static final float MAX = 12.5f / 16;
    private static final float BOTTOM = 0.5f / 16;
    private static final float FULL_HEIGHT = 11f / 16;

    static final class State extends BlockEntityRenderState {
        float fill;
        int color;
    }

    JarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(JarBlockEntity jar, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(jar, state, partialTick, camera, crumbling);
        int total = jar.contents().aspects().total();
        state.fill = Math.min(1f, (float) total / Math.max(1, jar.displayCapacity()));
        state.color = AspectColors.mix(jar.contents().aspects());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.fill <= 0) {
            return;
        }
        float top = BOTTOM + FULL_HEIGHT * state.fill;
        int argb = 0xD0000000 | state.color;
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, LIQUID, (p, consumer) -> {
            // Top, then the four sides; the bottom sits on the glass and is never seen.
            quad(consumer, p, argb, light, 0, 1, 0, MIN, top, MIN, MIN, top, MAX, MAX, top, MAX, MAX, top, MIN);
            quad(consumer, p, argb, light, 0, 0, -1, MIN, BOTTOM, MIN, MIN, top, MIN, MAX, top, MIN, MAX, BOTTOM, MIN);
            quad(consumer, p, argb, light, 0, 0, 1, MAX, BOTTOM, MAX, MAX, top, MAX, MIN, top, MAX, MIN, BOTTOM, MAX);
            quad(consumer, p, argb, light, -1, 0, 0, MIN, BOTTOM, MAX, MIN, top, MAX, MIN, top, MIN, MIN, BOTTOM, MIN);
            quad(consumer, p, argb, light, 1, 0, 0, MAX, BOTTOM, MIN, MAX, top, MIN, MAX, top, MAX, MAX, BOTTOM, MAX);
        });
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int argb, int light, float nx, float ny, float nz,
            float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        vertex(consumer, pose, argb, light, nx, ny, nz, x1, y1, z1, 0, 1);
        vertex(consumer, pose, argb, light, nx, ny, nz, x2, y2, z2, 0, 0);
        vertex(consumer, pose, argb, light, nx, ny, nz, x3, y3, z3, 1, 0);
        vertex(consumer, pose, argb, light, nx, ny, nz, x4, y4, z4, 1, 1);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int argb, int light, float nx, float ny, float nz,
            float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
