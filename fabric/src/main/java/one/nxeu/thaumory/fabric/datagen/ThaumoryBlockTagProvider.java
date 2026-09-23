package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;

final class ThaumoryBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {
    ThaumoryBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Campfires count only while lit; the Crucible checks that itself.
        var heatSources = builder(CrucibleBlockEntity.HEAT_SOURCES);
        for (Block block : List.of(Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE)) {
            heatSources.add(key(block));
        }
        builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ThaumoryBlocks.CRUCIBLE.get()));
    }

    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }
}
