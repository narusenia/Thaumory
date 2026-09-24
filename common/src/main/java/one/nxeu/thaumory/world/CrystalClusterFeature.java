package one.nxeu.thaumory.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;

/**
 * A small group of crystals on the floors, walls and ceilings of a cave (requirements §17.3). From
 * an open cell it looks out in each direction for a surface to grow on; around the first crystal,
 * the rest of the group go on any open cell next to such a surface.
 *
 * @param block          a facing crystal such as {@link AmethystClusterBlock}
 * @param searchRange    how far to look from the starting cell for the first surface
 * @param size           how many crystals to try for in the group
 * @param spread         how far from the first crystal the others may be
 * @param canBePlacedOn  the blocks crystals grow on
 */
public record CrystalClusterFeature(Block block, int searchRange, IntProvider size, int spread, HolderSet<Block> canBePlacedOn)
        implements Feature {
    public static final MapCodec<CrystalClusterFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(CrystalClusterFeature::block),
            Codec.intRange(1, 32).optionalFieldOf("search_range", 8).forGetter(CrystalClusterFeature::searchRange),
            IntProviders.codec(1, 64).fieldOf("size").forGetter(CrystalClusterFeature::size),
            Codec.intRange(0, 8).optionalFieldOf("spread", 3).forGetter(CrystalClusterFeature::spread),
            RegistryCodecs.holderSet(Registries.BLOCK).fieldOf("can_be_placed_on").forGetter(CrystalClusterFeature::canBePlacedOn)
    ).apply(i, CrystalClusterFeature::new));

    @Override
    public MapCodec<CrystalClusterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!open(level.getBlockState(origin))) {
            return false;
        }
        BlockPos first = firstSpot(level, random, origin);
        if (first == null) {
            return false;
        }
        int wanted = size.sample(random) - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < wanted * 4 && wanted > 0; attempt++) {
            pos.setWithOffset(first, random.nextIntBetweenInclusive(-spread, spread), random.nextIntBetweenInclusive(-spread, spread),
                    random.nextIntBetweenInclusive(-spread, spread));
            if (grow(level, random, pos)) {
                wanted--;
            }
        }
        return true;
    }

    /** Walks out from {@code origin} until it grows the first crystal. */
    private BlockPos firstSpot(WorldGenLevel level, RandomSource random, BlockPos origin) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.allShuffled(random)) {
            pos.set(origin);
            for (int step = 0; step < searchRange; step++) {
                if (!open(level.getBlockState(pos))) {
                    break;
                }
                if (grow(level, random, pos)) {
                    return pos.immutable();
                }
                pos.move(direction);
            }
        }
        return null;
    }

    /** Grows a crystal at {@code pos} off a neighboring surface, if it is open and has one. */
    private boolean grow(WorldGenLevel level, RandomSource random, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if (!open(here) || here.is(block)) {
            return false;
        }
        for (Direction front : Direction.allShuffled(random)) {
            BlockPos behind = pos.relative(front.getOpposite());
            if (!level.getBlockState(behind).is(canBePlacedOn)) {
                continue;
            }
            BlockState crystal = block.defaultBlockState()
                    .trySetValue(AmethystClusterBlock.FACING, front)
                    .trySetValue(AmethystClusterBlock.WATERLOGGED, here.getFluidState().is(Fluids.WATER));
            if (crystal.canSurvive(level, pos)) {
                level.setBlock(pos, crystal, Block.UPDATE_CLIENTS);
                return true;
            }
        }
        return false;
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) && state.getFluidState().isSource();
    }
}
