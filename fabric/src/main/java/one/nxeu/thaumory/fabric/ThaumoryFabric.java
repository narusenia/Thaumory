package one.nxeu.thaumory.fabric;

import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.ThaumoryPlatform;
import one.nxeu.thaumory.essentia.EssentiaLookup;
import one.nxeu.thaumory.fabric.transfer.EssentiaStorage;
import one.nxeu.thaumory.fabric.transfer.FabricEssentiaLookup;
import one.nxeu.thaumory.flux.FluxStorage;
import one.nxeu.thaumory.knowledge.KnowledgeStorage;
import one.nxeu.thaumory.world.ThaumoryFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class ThaumoryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Thaumory.init(new Platform(new FabricFluxStorage(), new FabricKnowledgeStorage(), new FabricEssentiaLookup()));
        EssentiaStorage.register();
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION,
                ThaumoryFeatures.ARCANE_CRYSTAL);
    }

    private record Platform(FluxStorage fluxStorage, KnowledgeStorage knowledgeStorage, EssentiaLookup essentiaLookup)
            implements ThaumoryPlatform {}
}
