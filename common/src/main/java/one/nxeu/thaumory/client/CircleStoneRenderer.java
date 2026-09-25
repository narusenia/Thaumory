package one.nxeu.thaumory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.block.stone.CircleStoneBlock;
import one.nxeu.thaumory.block.stone.CircleStoneBlockEntity;
import one.nxeu.thaumory.knowledge.CircleCombination;

/**
 * While a circle stone works, two of the Core's turning rings float over it, in the colours of the
 * circle's two effect aspects, bobbing a little. A resting stone shows none.
 */
final class CircleStoneRenderer implements BlockEntityRenderer<CircleStoneBlockEntity, CircleStoneRenderer.State> {
    /** Over the tablet, which is 3/16 thick. */
    private static final float HEIGHT = 0.5f;
    private static final float BOB = 0.05f;
    /** The outer ring a little above the inner, so the two never fight over the same depth. */
    private static final float STEP = 0.01f;
    private static final float HALF = 0.5f;
    private static final int UNKNOWN_COLOR = 0xAAAAAA;

    static final class State extends BlockEntityRenderState {
        boolean running;
        int[] colors = new int[0];
        Direction front = Direction.UP;
        float time;
    }

    CircleStoneRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(CircleStoneBlockEntity stone, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(stone, state, partialTick, camera, crumbling);
        state.running = stone.isRunning() && stone.burnt().isPresent();
        if (state.running) {
            CircleCombination combination = stone.burnt().get().combination();
            state.colors = new int[] {color(combination.first()), color(combination.second())};
        }
        state.front = stone.getBlockState().getValue(CircleStoneBlock.FACING);
        state.time = stone.getLevel() == null ? 0 : stone.getLevel().getGameTime() + partialTick;
    }

    private static int color(Identifier aspect) {
        return ThaumoryApi.aspects().get(aspect).map(Aspect::color).orElse(UNKNOWN_COLOR);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.running) {
            return;
        }
        float height = HEIGHT + BOB * (float) Math.sin(state.time / 20);
        for (int i = 0; i < state.colors.length; i++) {
            CircleCoreRenderer.turningRing(pose, collector, state.front, state.time, i, state.colors[i], height + STEP * i, HALF);
        }
    }
}
