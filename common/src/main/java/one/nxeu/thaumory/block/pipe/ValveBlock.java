package one.nxeu.thaumory.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import one.nxeu.thaumory.pipe.PipeNetworks;
import org.jspecify.annotations.Nullable;

/**
 * A pipe that a redstone signal closes (requirements §8.2). Closed, it is no part of any network,
 * so the network parts there; what it carried stays in it until it opens again.
 */
public final class ValveBlock extends EssentiaPipeBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public boolean carries(BlockState state) {
        return !state.getValue(POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    /** Placed some other way than by hand: take up the signal already there. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        super.onPlace(state, level, pos, old, movedByPiston);
        if (level instanceof ServerLevel server) {
            BlockState current = level.getBlockState(pos);
            boolean powered = level.hasNeighborSignal(pos);
            if (current.is(this) && current.getValue(POWERED) != powered) {
                level.setBlock(pos, current.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
                PipeNetworks.of(server).invalidate(pos);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        if (level instanceof ServerLevel server) {
            BlockState current = level.getBlockState(pos);
            boolean powered = level.hasNeighborSignal(pos);
            if (current.getValue(POWERED) != powered) {
                level.setBlock(pos, current.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
                PipeNetworks.of(server).invalidate(pos);
            }
        }
    }
}
