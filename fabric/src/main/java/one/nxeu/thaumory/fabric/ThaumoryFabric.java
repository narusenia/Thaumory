package one.nxeu.thaumory.fabric;

import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.ThaumoryPlatform;
import one.nxeu.thaumory.flux.FluxStorage;
import net.fabricmc.api.ModInitializer;

public final class ThaumoryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FluxStorage fluxStorage = new FabricFluxStorage();
        Thaumory.init(new ThaumoryPlatform() {
            @Override
            public FluxStorage fluxStorage() {
                return fluxStorage;
            }
        });
    }
}
