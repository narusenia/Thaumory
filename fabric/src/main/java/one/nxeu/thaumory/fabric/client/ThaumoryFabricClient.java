package one.nxeu.thaumory.fabric.client;

import java.util.function.Function;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import one.nxeu.thaumory.client.MenuScreenFactories;
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
        }, new MenuScreenFactories() {
            @Override
            public <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<M> type, Factory<M, S> factory) {
                MenuScreens.register(type, factory::create);
            }
        });
    }
}
