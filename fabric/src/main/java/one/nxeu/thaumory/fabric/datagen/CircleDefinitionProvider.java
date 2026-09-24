package one.nxeu.thaumory.fabric.datagen;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BESTIA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.CHAOS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.MORS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ORDO;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TEMPESTAS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.UMBRA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VINCULUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VITA;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.circle.CircleDefinitionFile;
import one.nxeu.thaumory.circle.CircleMode;
import one.nxeu.thaumory.circle.InfusionCost;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;

/** Writes {@code data/thaumory/thaumory/circle/*.json}, the built-in combinations (requirements §4.5). */
final class CircleDefinitionProvider extends FabricCodecDataProvider<CircleDefinitionFile> {
    private static final int SUSTAINED_INTERVAL = 200;

    CircleDefinitionProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/circle", CircleDefinitionFile.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, CircleDefinitionFile> output, HolderLookup.Provider registries) {
        triggered(output, ThaumoryCircleEffects.TELEPORT, ARCANUM, AER, List.of(CircleDefinitionFile.ANY), 4);
        sustained(output, ThaumoryCircleEffects.LIGHT, LUX, IGNIS, slot3(true, UMBRA), Map.of());
        sustained(output, ThaumoryCircleEffects.PURIFICATION, ORDO, LUX, slot3(true, IGNIS, AER, VITA, AQUA, TERRA, MORS),
                Map.of("flux_per_second", 0.5, "flux_per_essentia", 2.0, "restore_per_second", 2.0));
        sustained(output, ThaumoryCircleEffects.WARD, VINCULUM, ORDO, slot3(false, BESTIA, MORS, CHAOS), Map.of());
        sustained(output, ThaumoryCircleEffects.GROWTH, HERBA, VITA, slot3(false, HERBA, BESTIA), Map.of());
        sustained(output, ThaumoryCircleEffects.HEALING, VITA, ORDO, slot3(true, BESTIA), Map.of());
        sustained(output, ThaumoryCircleEffects.ATTRACTION, TEMPESTAS, VINCULUM,
                List.of(CircleDefinitionFile.NONE, CircleDefinitionFile.ANY), Map.of());
        triggered(output, ThaumoryCircleEffects.WEATHER, TEMPESTAS, ARCANUM, slot3(false, AQUA, IGNIS, TEMPESTAS), 8);
    }

    private static void triggered(BiConsumer<Identifier, CircleDefinitionFile> output, Identifier effect, Aspect first, Aspect second,
            List<String> slot3, int cost) {
        output.accept(effect, new CircleDefinitionFile(effect, List.of(first.id(), second.id()), slot3, CircleMode.TRIGGERED,
                cost, SUSTAINED_INTERVAL, Map.of(), InfusionCost.DEFAULT));
    }

    private static void sustained(BiConsumer<Identifier, CircleDefinitionFile> output, Identifier effect, Aspect first, Aspect second,
            List<String> slot3, Map<String, Double> settings) {
        output.accept(effect, new CircleDefinitionFile(effect, List.of(first.id(), second.id()), slot3, CircleMode.SUSTAINED,
                1, SUSTAINED_INTERVAL, settings, InfusionCost.DEFAULT));
    }

    private static List<String> slot3(boolean empty, Aspect... aspects) {
        List<String> entries = new ArrayList<>();
        if (empty) {
            entries.add(CircleDefinitionFile.NONE);
        }
        for (Aspect aspect : aspects) {
            entries.add(aspect.id().toString());
        }
        return entries;
    }

    @Override
    public String getName() {
        return "Thaumory circle combinations";
    }
}
