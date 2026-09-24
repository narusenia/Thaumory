package one.nxeu.thaumory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.block.pedestal.PedestalBlockEntity;

/** Draws the item on a pedestal floating over it and slowly turning. */
final class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity, PedestalRenderer.State> {
    private final ItemModelResolver items;

    static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        float time;
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
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5f, 1.05f + 0.04f * (float) Math.sin(state.time / 12), 0.5f);
        pose.rotateDegrees(Axis.YP, (state.time * 1.5f) % 360);
        pose.scale(0.75f, 0.75f, 0.75f);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
}
