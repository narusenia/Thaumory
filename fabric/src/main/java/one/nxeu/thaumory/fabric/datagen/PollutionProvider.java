package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.flux.pollution.PollutionFile;

/** Writes {@code data/thaumory/thaumory/pollution/*.json}: soil and stone (requirements §5.2). */
final class PollutionProvider extends FabricCodecDataProvider<PollutionFile> {
    PollutionProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/pollution", PollutionFile.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, PollutionFile> output, HolderLookup.Provider registries) {
        rule(output, ThaumoryBlocks.POLLUTED_SOIL.get(), List.of("#" + BlockTags.DIRT.location(), "#" + BlockTags.GRASS_BLOCKS.location(),
                id(Blocks.DIRT_PATH), id(Blocks.FARMLAND)),
                Blocks.DIRT);
        rule(output, ThaumoryBlocks.POLLUTED_STONE.get(), List.of("#" + BlockTags.BASE_STONE_OVERWORLD.location(), id(Blocks.COBBLESTONE),
                id(Blocks.COBBLED_DEEPSLATE)), Blocks.STONE);
    }

    private static void rule(BiConsumer<Identifier, PollutionFile> output, Block polluted, List<String> from, Block restore) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(polluted);
        output.accept(key, new PollutionFile(key, from, BuiltInRegistries.BLOCK.getKey(restore)));
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    @Override
    public String getName() {
        return "Thaumory pollution rules";
    }
}
