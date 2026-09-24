package one.nxeu.thaumory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.pipe.EssentiaPipeBlock;
import one.nxeu.thaumory.block.pipe.PipeBlockEntity;
import one.nxeu.thaumory.pipe.PipeDisplay;

/**
 * Draws the Essentia in transit as liquid inside a pipe's glass (requirements §8.2): in the joint
 * and every arm, as high as the network is full; arms going up or down are always full.
 */
final class PipeRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeRenderer.State> {
    private static final RenderType LIQUID = RenderTypes.entityTranslucent(Thaumory.id("textures/block/jar_liquid.png"));
    // Just inside the glass: the arms are 5.5..10.5 across, the joint 5..11.
    private static final float LOW = 6f / 16;
    private static final float HIGH = 10f / 16;

    static final class State extends BlockEntityRenderState {
        int color;
        int level;
        final Set<Direction> arms = EnumSet.noneOf(Direction.class);
    }

    PipeRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PipeBlockEntity pipe, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTick, camera, crumbling);
        PipeDisplay display = pipe.display();
        state.color = display.color();
        state.level = display.level();
        state.arms.clear();
        BlockState block = pipe.getBlockState();
        for (var connection : EssentiaPipeBlock.CONNECTIONS.entrySet()) {
            if (block.hasProperty(connection.getValue()) && block.getValue(connection.getValue())) {
                state.arms.add(connection.getKey());
            }
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.level <= 0) {
            return;
        }
        float top = LOW + (HIGH - LOW) * state.level / PipeDisplay.STEPS;
        int argb = 0xD0000000 | state.color;
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, LIQUID, (p, consumer) -> {
            // The joint, open towards each arm so no inner walls show through the liquid.
            box(consumer, p, argb, light, LOW, LOW, LOW, HIGH, state.arms.contains(Direction.UP) ? HIGH : top, HIGH, state.arms);
            for (Direction arm : state.arms) {
                Set<Direction> open = EnumSet.of(arm, arm.getOpposite());
                switch (arm) {
                    case NORTH -> box(consumer, p, argb, light, LOW, LOW, 0, HIGH, top, LOW, open);
                    case SOUTH -> box(consumer, p, argb, light, LOW, LOW, HIGH, HIGH, top, 1, open);
                    case WEST -> box(consumer, p, argb, light, 0, LOW, LOW, LOW, top, HIGH, open);
                    case EAST -> box(consumer, p, argb, light, HIGH, LOW, LOW, 1, top, HIGH, open);
                    case DOWN -> box(consumer, p, argb, light, LOW, 0, LOW, HIGH, LOW, HIGH, open);
                    case UP -> box(consumer, p, argb, light, LOW, HIGH, LOW, HIGH, 1, HIGH, open);
                }
            }
        });
    }

    /** A box of liquid, leaving out the faces towards {@code open}. */
    private static void box(VertexConsumer c, PoseStack.Pose p, int argb, int light,
            float x1, float y1, float z1, float x2, float y2, float z2, Set<Direction> open) {
        if (!open.contains(Direction.UP)) {
            quad(c, p, argb, light, 0, 1, 0, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        }
        if (!open.contains(Direction.DOWN)) {
            quad(c, p, argb, light, 0, -1, 0, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2);
        }
        if (!open.contains(Direction.NORTH)) {
            quad(c, p, argb, light, 0, 0, -1, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        }
        if (!open.contains(Direction.SOUTH)) {
            quad(c, p, argb, light, 0, 0, 1, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2);
        }
        if (!open.contains(Direction.WEST)) {
            quad(c, p, argb, light, -1, 0, 0, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1);
        }
        if (!open.contains(Direction.EAST)) {
            quad(c, p, argb, light, 1, 0, 0, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
        }
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
