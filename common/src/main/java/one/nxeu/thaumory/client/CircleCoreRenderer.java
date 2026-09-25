package one.nxeu.thaumory.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CircleChildren;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.circle.CircleSide;

/**
 * Draws a ring around the Core's centre mark for each rune, in that rune's aspect color. The rings
 * glow, with their light bleeding softly around them, and turn slowly, each the other way from the one inside it, on whichever face the Core is
 * drawn on. With a pedestal built in, a
 * ring of glyphs also turns around its column, and the item on it floats over the top.
 */
final class CircleCoreRenderer implements BlockEntityRenderer<CircleCoreBlockEntity, CircleCoreRenderer.State> {
    private static final List<RenderType> RINGS = List.of(
            RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/circle_core_ring_1.png")),
            RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/circle_core_ring_2.png")),
            RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/circle_core_ring_3.png")));
    /** The light that bleeds out around each ring, made from the ring itself (see {@link RingGlows}). */
    private static final List<RenderType> GLOWS = List.of(
            RenderTypes.entityTranslucentEmissive(RingGlows.glow(0)),
            RenderTypes.entityTranslucentEmissive(RingGlows.glow(1)),
            RenderTypes.entityTranslucentEmissive(RingGlows.glow(2)));
    private static final float GLOW_ALPHA = 0.28f;
    /** How far the glow's colour leans towards white. */
    private static final float GLOW_WHITEN = 0.1f;
    /** Degrees per tick, inner ring first. */
    private static final float[] SPEEDS = {1.2f, -0.8f, 0.5f, -0.35f};
    /** For a rune whose aspect is no longer registered. */
    private static final int UNKNOWN_COLOR = 0xAAAAAA;
    // Just above chalk (0.25/16), each ring a little higher so they never z-fight.
    private static final float BASE_HEIGHT = 0.3f / 16;
    private static final float RING_STEP = 0.05f / 16;

    /** How much larger the turning circles are at each rank, rank 1 first (requirements §4.6). */
    private static final float[] RANK_SCALES = {1.0f, 1.2f, 1.4f};
    /** A fourth rune's ring is the third's picture, drawn this much wider around it. */
    private static final float OUTER_RING_SCALE = 1.3f;

    /** The emblem over a working circle: soft lines tracing its rings, and spots on its nodes. */
    private static final RenderType EMBLEM_LINE = RenderTypes.entityTranslucentEmissive(RingGlows.LINE);
    private static final RenderType EMBLEM_DOT = RenderTypes.entityTranslucentEmissive(RingGlows.DOT);
    private static final float EMBLEM_ALPHA = 0.4f;
    private static final float EMBLEM_WIDTH = 0.35f;
    private static final float EMBLEM_DOT_SIZE = 0.7f;
    /** Above the face it is drawn on, bobbing by {@link #EMBLEM_BOB}. */
    private static final float EMBLEM_HEIGHT = 0.3f;
    private static final float EMBLEM_BOB = 0.06f;
    /** How far a sub-circle's ring floats above its parent's emblem, so the two never fight over the same depth. */
    private static final float SUB_EMBLEM_LIFT = 0.02f;
    /** Ticks a triggered circle's emblem takes to fade. */
    private static final float FLASH_TICKS = 20;

    private static final RenderType GLYPH_RING = RenderTypes.entityTranslucentEmissive(Thaumory.id("textures/block/pedestal_ring.png"));
    private static final int GLYPH_RING_COLOR = 0xE0B070FF;
    private static final float GLYPH_RING_HEIGHT = 6f / 16;
    private static final float GLYPH_RING_HALF = 0.45f;

    static final class State extends BlockEntityRenderState {
        int[] colors = new int[0];
        float time;
        boolean pedestal;
        Direction front = Direction.UP;
        int rank = 1;
        int rings;
        /** For a sub-circle: the parent's ring it sits on, and where the parent is from here; 0 otherwise. */
        int seatRing;
        int parentX;
        int parentZ;
        /** 0 to 1: how strongly the emblem shows. */
        float emblem;
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
        state.front = core.front();
        state.rank = core.rank();
        state.rings = core.scan().rings();
        state.seatRing = 0;
        Optional<CircleSide> side = core.seatSide();
        if (core.seat().filter(CircleChildren.Seat.CHILD::equals).isPresent() && side.isPresent()) {
            state.seatRing = core.frameRings();
            state.parentX = -side.get().nodeX(state.seatRing);
            state.parentZ = -side.get().nodeZ(state.seatRing);
        }
        float sinceFlash = core.lastFlash() == Long.MIN_VALUE ? Float.MAX_VALUE : state.time - core.lastFlash();
        state.emblem = core.isRunning() ? 1 : Math.max(0, 1 - sinceFlash / FLASH_TICKS);
        items.updateForTopItem(state.item, core.pedestalItem(), ItemDisplayContext.GROUND, core.getLevel(), null,
                (int) core.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float rankScale = RANK_SCALES[Math.clamp(state.rank - 1, 0, RANK_SCALES.length - 1)];
        for (int i = 0; i < state.colors.length && i < SPEEDS.length; i++) {
            float half = 0.5f * rankScale * (i >= RINGS.size() ? OUTER_RING_SCALE : 1);
            turningRing(pose, collector, state.front, state.time, i, state.colors[i], BASE_HEIGHT + RING_STEP * i, half);
        }
        if (state.emblem > 0 && state.colors.length > 0) {
            if (state.seatRing > 0) {
                // A sub-circle lights only the parent's ring it sits on, a little above the parent's own emblem.
                emblem(state, pose, collector, state.seatRing, state.seatRing, state.parentX, state.parentZ, SUB_EMBLEM_LIFT);
            } else if (state.rings > 0) {
                emblem(state, pose, collector, 1, state.rings, 0, 0, 0);
            }
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

    /**
     * The {@code i}th turning ring (inner first: its picture and speed) in {@code rgb}, with its glow,
     * {@code height} above the face that {@code front} points away from and {@code half} blocks from
     * its centre to its edge. Circle stones draw theirs this way too.
     */
    static void turningRing(PoseStack pose, SubmitNodeCollector collector, Direction front, float time, int i, int rgb, float height,
            float half) {
        int argb = 0xFF000000 | rgb;
        int picture = Math.min(i, RINGS.size() - 1);
        pose.pushPose();
        pose.rotateAround(front.getRotation(), 0.5f, 0.5f, 0.5f);
        pose.translate(0.5f, 0, 0.5f);
        pose.rotateDegrees(Axis.YP, (time * SPEEDS[i % SPEEDS.length]) % 360);
        collector.submitCustomGeometry(pose, RINGS.get(picture), (p, consumer) -> quad(consumer, p, argb, height, half));
        // Just under its ring, so the two never fight over the same depth.
        int glow = glow(rgb, GLOW_ALPHA);
        float glowHeight = height - RING_STEP / 2;
        collector.submitCustomGeometry(pose, GLOWS.get(picture), (p, consumer) -> quad(consumer, p, glow, glowHeight, half * RingGlows.scale()));
        pose.popPose();
    }

    /**
     * Soft lines along the circles of rings {@code from} to {@code to} of the circle centred
     * ({@code x}, {@code z}) from this Core, where their chalk runs, and a spot on each of their nodes.
     */
    private static void emblem(State state, PoseStack pose, SubmitNodeCollector collector, int from, int to, int x, int z, float lift) {
        int color = glow(state.colors[0], EMBLEM_ALPHA * state.emblem);
        float y = EMBLEM_HEIGHT + lift + EMBLEM_BOB * (float) Math.sin(state.time / 30);
        float w = EMBLEM_WIDTH / 2;
        float d = EMBLEM_DOT_SIZE / 2;
        pose.pushPose();
        pose.rotateAround(state.front.getRotation(), 0.5f, 0.5f, 0.5f);
        pose.translate(0.5f + x, 0, 0.5f + z);
        collector.submitCustomGeometry(pose, EMBLEM_LINE, (p, consumer) -> {
            for (int ring = from; ring <= to; ring++) {
                circle(consumer, p, color, y, CircleScan.radius(ring), w);
            }
        });
        collector.submitCustomGeometry(pose, EMBLEM_DOT, (p, consumer) -> {
            for (int ring = from; ring <= to; ring++) {
                for (CircleSide side : CircleSide.values()) {
                    int nx = side.nodeX(ring);
                    int nz = side.nodeZ(ring);
                    spot(consumer, p, color, y + 0.001f, nx - d, nz - d, nx + d, nz + d);
                }
            }
        });
        pose.popPose();
    }

    /** A flat circle of radius {@code radius} and half width {@code w}, in straight pieces; the texture's V runs across it. */
    private static void circle(VertexConsumer consumer, PoseStack.Pose pose, int argb, float y, int radius, float w) {
        int segments = 8 * radius;
        float inner = radius - w;
        float outer = radius + w;
        for (int i = 0; i < segments; i++) {
            double a0 = 2 * Math.PI * i / segments;
            double a1 = 2 * Math.PI * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            // Wound to face up, like the other quads.
            vertex(consumer, pose, argb, inner * c0, y, inner * s0, 0, 0);
            vertex(consumer, pose, argb, inner * c1, y, inner * s1, 1, 0);
            vertex(consumer, pose, argb, outer * c1, y, outer * s1, 1, 1);
            vertex(consumer, pose, argb, outer * c0, y, outer * s0, 0, 1);
        }
    }

    /** A flat quad from (x0, z0) to (x1, z1), the texture's U along x and V along z. */
    private static void spot(VertexConsumer consumer, PoseStack.Pose pose, int argb, float y, float x0, float z0, float x1, float z1) {
        vertex(consumer, pose, argb, x0, y, z0, 0, 0);
        vertex(consumer, pose, argb, x0, y, z1, 0, 1);
        vertex(consumer, pose, argb, x1, y, z1, 1, 1);
        vertex(consumer, pose, argb, x1, y, z0, 1, 0);
    }

    /** The emblem reaches well past the Core's own block. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
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

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int argb, float height, float half) {
        vertex(consumer, pose, argb, -half, height, -half, 0, 0);
        vertex(consumer, pose, argb, -half, height, half, 0, 1);
        vertex(consumer, pose, argb, half, height, half, 1, 1);
        vertex(consumer, pose, argb, half, height, -half, 1, 0);
    }

    /** {@code rgb} leant towards white, at {@code alpha}. */
    private static int glow(int rgb, float alpha) {
        int r = whiten((rgb >> 16) & 0xFF);
        int g = whiten((rgb >> 8) & 0xFF);
        int b = whiten(rgb & 0xFF);
        return Math.round(alpha * 255) << 24 | r << 16 | g << 8 | b;
    }

    private static int whiten(int channel) {
        return Math.round(channel + (255 - channel) * GLOW_WHITEN);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int argb, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
