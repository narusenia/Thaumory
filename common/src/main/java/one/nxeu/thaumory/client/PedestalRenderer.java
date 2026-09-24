package one.nxeu.thaumory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.pedestal.PedestalBlockEntity;

/**
 * Draws the item on a pedestal floating over it and slowly turning, and, while the pedestal
 * stands on a Core, a glowing ring of glyphs turning around its column.
 */
final class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity, PedestalRenderer.State> {
    private static final RenderType RING = RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/pedestal_ring.png"));
    private static final int RING_COLOR = 0xE0B070FF;
    private static final float RING_HEIGHT = 6f / 16;
    private static final float RING_HALF = 0.45f;

    private final ItemModelResolver items;

    static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
        boolean onCore;
    }

    PedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PedestalBlockEntity pedestal, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pedestal, state, partialTick, camera, crumbling);
        items.updateForTopItem(state.item, pedestal.item(), ItemDisplayContext.GROUND, pedestal.getLevel(), null,
                (int) pedestal.getBlockPos().asLong());
        state.time = pedestal.getLevel() == null ? 0 : pedestal.getLevel().getGameTime() + partialTick;
        state.onCore = pedestal.getLevel() != null && pedestal.getLevel().getBlockEntity(pedestal.getBlockPos().below()) instanceof CircleCoreBlockEntity;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.onCore) {
            ring(state, pose, collector);
        }
        if (state.item.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5f, 0.95f + 0.04f * (float) Math.sin(state.time / 12), 0.5f);
        pose.rotateDegrees(Axis.YP, (state.time * 1.5f) % 360);
        pose.scale(0.75f, 0.75f, 0.75f);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    /** Seen from above and below alike, bobbing a little as it turns. */
    private static void ring(State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.pushPose();
        pose.translate(0.5f, RING_HEIGHT + 0.02f * (float) Math.sin(state.time / 20), 0.5f);
        pose.rotateDegrees(Axis.YP, (state.time * 0.8f) % 360);
        collector.submitCustomGeometry(pose, RING, (p, consumer) -> {
            vertex(consumer, p, -RING_HALF, -RING_HALF, 0, 0, 1);
            vertex(consumer, p, -RING_HALF, RING_HALF, 0, 1, 1);
            vertex(consumer, p, RING_HALF, RING_HALF, 1, 1, 1);
            vertex(consumer, p, RING_HALF, -RING_HALF, 1, 0, 1);
            vertex(consumer, p, RING_HALF, -RING_HALF, 1, 0, -1);
            vertex(consumer, p, RING_HALF, RING_HALF, 1, 1, -1);
            vertex(consumer, p, -RING_HALF, RING_HALF, 0, 1, -1);
            vertex(consumer, p, -RING_HALF, -RING_HALF, 0, 0, -1);
        });
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float u, float v, float normal) {
        consumer.addVertex(pose, x, 0, z).setColor(RING_COLOR).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, normal, 0);
    }
}
