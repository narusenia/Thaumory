package one.nxeu.thaumory.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import one.nxeu.thaumory.Thaumory;
import org.slf4j.Logger;

/**
 * The light around the Core's rings (requirements §4.1), made from the ring textures each time
 * resources load, so a redrawn ring gets a matching glow: white, with the ring's alpha blurred out
 * over a margin.
 */
final class RingGlows implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int RINGS = 3;
    /** Pixels of margin per 64 of texture, so the light can reach past the ring's edge. */
    private static final int MARGIN = 8;
    private static final int RADIUS = 2;
    private static final int SIZE = 64;

    static Identifier ring(int index) {
        return Thaumory.id("textures/block/circle_core_ring_" + (index + 1) + ".png");
    }

    /** A soft line of light, full along its length and fading out across it. */
    static final Identifier LINE = Thaumory.id("dynamic/emblem_line");
    /** A soft round spot of light. */
    static final Identifier DOT = Thaumory.id("dynamic/emblem_dot");

    static Identifier glow(int index) {
        return Thaumory.id("dynamic/circle_core_ring_" + (index + 1) + "_glow");
    }

    /** How much larger the glow's quad is than the ring's, for the margin around it. */
    static float scale() {
        return (SIZE + 2f * MARGIN) / SIZE;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resources) {
        register(LINE, soft(16, false));
        register(DOT, soft(16, true));
        for (int i = 0; i < RINGS; i++) {
            Optional<Resource> resource = resources.getResource(ring(i));
            if (resource.isEmpty()) {
                continue;
            }
            try (InputStream in = resource.get().open(); NativeImage ring = NativeImage.read(in)) {
                Identifier id = glow(i);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, glowOf(ring)));
            } catch (IOException e) {
                LOGGER.warn("Could not make the glow for {}", ring(i), e);
            }
        }
    }

    private static void register(Identifier id, NativeImage image) {
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(id::toString, image));
    }

    /** White with a soft falloff: across the V axis for a line, from the centre for a spot. */
    private static NativeImage soft(int size, boolean round) {
        NativeImage image = new NativeImage(size, size, false);
        double centre = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dy = (y - centre) / centre;
                double dx = round ? (x - centre) / centre : 0;
                double d2 = dx * dx + dy * dy;
                int a = (int) Math.round(255 * Math.exp(-d2 * 4));
                image.setPixel(x, y, a << 24 | 0xFFFFFF);
            }
        }
        return image;
    }

    private static NativeImage glowOf(NativeImage ring) {
        int width = ring.getWidth();
        int height = ring.getHeight();
        int margin = Math.max(1, MARGIN * width / SIZE);
        int radius = Math.max(1, RADIUS * width / SIZE);
        int[] alpha = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                alpha[y * width + x] = ring.getPixel(x, y) >>> 24;
            }
        }
        int[] mask = GlowMask.of(alpha, width, height, margin, radius);
        // Thin lines blur out faint; lift the brightest point to full.
        int peak = 1;
        for (int value : mask) {
            peak = Math.max(peak, value);
        }
        int w = width + 2 * margin;
        int h = height + 2 * margin;
        NativeImage glow = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = Math.min(255, mask[y * w + x] * 255 / peak);
                glow.setPixel(x, y, a << 24 | 0xFFFFFF);
            }
        }
        return glow;
    }
}
