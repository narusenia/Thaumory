package one.nxeu.thaumory.fabric;

import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.ThaumoryPlatform;
import one.nxeu.thaumory.essentia.EssentiaLookup;
import one.nxeu.thaumory.fabric.transfer.EssentiaStorage;
import one.nxeu.thaumory.fabric.transfer.FabricEssentiaLookup;
import one.nxeu.thaumory.flux.FluxStorage;
import one.nxeu.thaumory.knowledge.KnowledgeStorage;
import net.fabricmc.api.ModInitializer;

public final class ThaumoryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Thaumory.init(new Platform(new FabricFluxStorage(), new FabricKnowledgeStorage(), new FabricEssentiaLookup()));
        EssentiaStorage.register();
    }

    private record Platform(FluxStorage fluxStorage, KnowledgeStorage knowledgeStorage, EssentiaLookup essentiaLookup)
            implements ThaumoryPlatform {}
}
