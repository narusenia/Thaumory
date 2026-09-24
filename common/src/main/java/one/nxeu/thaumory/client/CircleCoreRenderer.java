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
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;

/**
 * Draws a ring around the Core's centre mark for each rune, in that rune's aspect color. The rings
 * glow and turn slowly, each the other way from the one inside it. With a pedestal built in, a
 * ring of glyphs also turns around its column, and the item on it floats over the top.
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

    private static final RenderType GLYPH_RING = RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/pedestal_ring.png"));
    private static final int GLYPH_RING_COLOR = 0xE0B070FF;
    private static final float GLYPH_RING_HEIGHT = 6f / 16;
    private static final float GLYPH_RING_HALF = 0.45f;

    static final class State extends BlockEntityRenderState {
        int[] colors = new int[0];
        float time;
        boolean pedestal;
        final ItemStackRenderState item = new ItemStackRenderState();
    }

    private final ItemModelResolver items;

    CircleCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

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
        state.pedestal = core.hasPedestal();
        items.updateForTopItem(state.item, core.pedestalItem(), ItemDisplayContext.GROUND, core.getLevel(), null,
                (int) core.getBlockPos().asLong());
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
        if (state.pedestal) {
            glyphRing(state, pose, collector);
        }
        if (!state.item.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5f, 0.95f + 0.04f * (float) Math.sin(state.time / 12), 0.5f);
            pose.rotateDegrees(Axis.YP, (state.time * 1.5f) % 360);
            pose.scale(0.75f, 0.75f, 0.75f);
            state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    /** Seen from above and below alike, bobbing a little as it turns. */
    private static void glyphRing(State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0.5f, GLYPH_RING_HEIGHT + 0.02f * (float) Math.sin(state.time / 20), 0.5f);
        pose.rotateDegrees(Axis.YP, (state.time * 0.8f) % 360);
        float h = GLYPH_RING_HALF;
        collector.submitCustomGeometry(pose, GLYPH_RING, (p, consumer) -> {
            glyphVertex(consumer, p, -h, -h, 0, 0, 1);
            glyphVertex(consumer, p, -h, h, 0, 1, 1);
            glyphVertex(consumer, p, h, h, 1, 1, 1);
            glyphVertex(consumer, p, h, -h, 1, 0, 1);
            glyphVertex(consumer, p, h, -h, 1, 0, -1);
            glyphVertex(consumer, p, h, h, 1, 1, -1);
            glyphVertex(consumer, p, -h, h, 0, 1, -1);
            glyphVertex(consumer, p, -h, -h, 0, 0, -1);
        });
        pose.popPose();
    }

    private static void glyphVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float u, float v, float normal) {
        consumer.addVertex(pose, x, 0, z).setColor(GLYPH_RING_COLOR).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, normal, 0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int argb, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
