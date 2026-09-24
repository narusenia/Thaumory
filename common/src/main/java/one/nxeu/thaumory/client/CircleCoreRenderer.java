package one.nxeu.thaumory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;

/**
 * Draws a ring around the Core's centre mark for each rune, in that rune's aspect color. The rings
 * glow and turn slowly, each the other way from the one inside it.
 */
final class CircleCoreRenderer implements BlockEntityRenderer<CircleCoreBlockEntity, CircleCoreRenderer.State> {
    private static final List<RenderType> RINGS = List.of(
            RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/circle_core_ring_1.png")),
            RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/circle_core_ring_2.png")),
            RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/circle_core_ring_3.png")));
    /** Degrees per tick, inner ring first. */
    private static final float[] SPEEDS = {1.2f, -0.8f, 0.5f};
    /** For a rune whose aspect is no longer registered. */
    private static final int UNKNOWN_COLOR = 0xAAAAAA;
    // Just above chalk (0.25/16), each ring a little higher so they never z-fight.
    private static final float BASE_HEIGHT = 0.3f / 16;
    private static final float RING_STEP = 0.05f / 16;

    static final class State extends BlockEntityRenderState {
        int[] colors = new int[0];
        float time;
    }

    CircleCoreRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CircleCoreBlockEntity core, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, camera, crumbling);
        List<Identifier> runes = core.runes();
        state.colors = new int[runes.size()];
        for (int i = 0; i < runes.size(); i++) {
            state.colors[i] = ThaumoryApi.aspects().get(runes.get(i)).map(Aspect::color).orElse(UNKNOWN_COLOR);
        }
        state.time = core.getLevel() == null ? 0 : core.getLevel().getGameTime() + partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.colors.length && i < RINGS.size(); i++) {
            int argb = 0xFF000000 | state.colors[i];
            float height = BASE_HEIGHT + RING_STEP * i;
            pose.pushPose();
            pose.translate(0.5f, 0, 0.5f);
            pose.rotateDegrees(Axis.YP, (state.time * SPEEDS[i]) % 360);
            collector.submitCustomGeometry(pose, RINGS.get(i), (p, consumer) -> {
                vertex(consumer, p, argb, -0.5f, height, -0.5f, 0, 0);
                vertex(consumer, p, argb, -0.5f, height, 0.5f, 0, 1);
                vertex(consumer, p, argb, 0.5f, height, 0.5f, 1, 1);
                vertex(consumer, p, argb, 0.5f, height, -0.5f, 1, 0);
            });
            pose.popPose();
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int argb, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
