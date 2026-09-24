package one.nxeu.thaumory.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/** {@code thaumory:aspect_mote}: a glowing speck in its aspect's colour, drifting up and fading out. */
public final class AspectMoteParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    private AspectMoteParticle(ClientLevel level, double x, double y, double z, ColorParticleOption colour, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.xd = (random.nextFloat() - 0.5) * 0.01;
        this.yd = 0.03 + random.nextFloat() * 0.02;
        this.zd = (random.nextFloat() - 0.5) * 0.01;
        this.friction = 0.95F;
        this.hasPhysics = false;
        this.lifetime = 20 + random.nextInt(16);
        this.quadSize *= 0.6F;
        setColor(colour.getRed(), colour.getGreen(), colour.getBlue());
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        alpha = Math.min(1, 2 * (1 - (float) age / lifetime));
    }

    @Override
    public int getLightCoords(float partialTick) {
        return LightCoordsUtil.withBlock(super.getLightCoords(partialTick), 15);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ColorParticleOption options, ClientLevel level, double x, double y, double z, double xAux,
                double yAux, double zAux, RandomSource random) {
            return new AspectMoteParticle(level, x, y, z, options, sprites);
        }
    }
}
