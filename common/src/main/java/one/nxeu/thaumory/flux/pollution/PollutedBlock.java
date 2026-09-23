package one.nxeu.thaumory.flux.pollution;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block Flux has turned bad. It keeps itself in the {@link PollutionIndex} wherever it is placed,
 * so a purification circle can find it again.
 */
public final class PollutedBlock extends Block {
    public PollutedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server) {
            PollutionIndex.of(server).add(pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (!(level.getBlockState(pos).getBlock() instanceof PollutedBlock)) {
            PollutionIndex.of(level).remove(pos);
        }
    }
}
