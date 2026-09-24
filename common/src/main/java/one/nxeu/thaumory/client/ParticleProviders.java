package one.nxeu.thaumory.client;

import java.util.function.Function;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * Where the loader registers the client side of our particle types. Architectury's
 * ParticleProviderRegistry does nothing on Fabric (its platform methods ship empty in 22.0.2), so
 * each loader hands its own registry to {@link ThaumoryClient#init}.
 */
public interface ParticleProviders {
    /** Registers {@code provider}, made from the sprites listed in {@code particles/<id>.json}, for {@code type}. */
    <T extends ParticleOptions> void register(ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider);
}
