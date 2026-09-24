package one.nxeu.thaumory.world;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import one.nxeu.thaumory.Thaumory;

/** Thaumory's world generation: the feature types, and the placed features each loader adds to biomes. */
public final class ThaumoryFeatures {
    private static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
            DeferredRegister.create(Thaumory.MOD_ID, Registries.FEATURE_TYPE);

    static {
        FEATURE_TYPES.register("crystal_cluster", () -> CrystalClusterFeature.CODEC);
    }

    /** Arcane crystals in the caves of the overworld; {@code data/thaumory/worldgen/placed_feature/arcane_crystal.json}. */
    public static final ResourceKey<PlacedFeature> ARCANE_CRYSTAL = ResourceKey.create(Registries.PLACED_FEATURE, Thaumory.id("arcane_crystal"));

    private ThaumoryFeatures() {}

    public static void register() {
        FEATURE_TYPES.register();
    }
}
