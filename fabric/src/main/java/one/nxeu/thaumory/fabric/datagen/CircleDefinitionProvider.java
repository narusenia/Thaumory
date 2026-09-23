package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.circle.CircleDefinitionFile;
import one.nxeu.thaumory.circle.CircleMode;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.UMBRA;

/** Writes {@code data/thaumory/thaumory/circle/*.json}, the built-in combinations (requirements §4.5). */
final class CircleDefinitionProvider extends FabricCodecDataProvider<CircleDefinitionFile> {
    CircleDefinitionProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/circle", CircleDefinitionFile.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, CircleDefinitionFile> output, HolderLookup.Provider registries) {
        output.accept(Thaumory.id("light"), new CircleDefinitionFile(ThaumoryCircleEffects.LIGHT, runes(LUX, IGNIS),
                Optional.of(List.of(Optional.empty(), Optional.of(UMBRA.id()))), CircleMode.SUSTAINED, 1, 200));
    }

    private static List<Identifier> runes(Aspect first, Aspect second) {
        return List.of(first.id(), second.id());
    }

    @Override
    public String getName() {
        return "Thaumory circle combinations";
    }
}
