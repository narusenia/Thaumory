package one.nxeu.thaumory.fabric.client;

import java.util.function.Function;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import one.nxeu.thaumory.client.ParticleProviders;
import one.nxeu.thaumory.client.ThaumoryClient;

public final class ThaumoryFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ThaumoryClient.init(new ParticleProviders() {
            @Override
            public <T extends ParticleOptions> void register(ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider) {
                ParticleProviderRegistry.getInstance().register(type, provider::apply);
            }
        });
    }
}
