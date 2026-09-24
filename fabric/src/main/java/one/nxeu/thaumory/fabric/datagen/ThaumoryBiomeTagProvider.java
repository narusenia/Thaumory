package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import one.nxeu.thaumory.Thaumory;

final class ThaumoryBiomeTagProvider extends FabricTagsProvider<Biome> {
    /** Where old circles stand (requirements §17.3): open land, never sea, river, peak or ice. */
    static final TagKey<Biome> HAS_OLD_CIRCLE = TagKey.create(Registries.BIOME, Thaumory.id("has_structure/old_circle"));

    ThaumoryBiomeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.BIOME, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var oldCircle = builder(HAS_OLD_CIRCLE);
        for (var biome : List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FOREST, Biomes.FLOWER_FOREST,
                Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
                Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.DESERT, Biomes.CHERRY_GROVE)) {
            oldCircle.add(biome);
        }
    }
}
