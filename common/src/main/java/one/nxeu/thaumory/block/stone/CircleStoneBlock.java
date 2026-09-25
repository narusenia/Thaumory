package one.nxeu.thaumory.block.stone;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import org.jspecify.annotations.Nullable;

/**
 * A circle stone (requirements §10.3): a small stone tablet with a circle burnt into it, laid on the
 * floor, a wall or the ceiling ({@link #FACING} points away from that face). It works on its own
 * while it holds Essentia, and rests while it has a redstone signal ({@link #POWERED}).
 */
public final class CircleStoneBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = ChalkPatternBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, box(2, 0, 2, 14, 3, 14),
            Direction.DOWN, box(2, 13, 2, 14, 16, 14),
            Direction.NORTH, box(2, 2, 13, 14, 14, 16),
            Direction.SOUTH, box(2, 2, 0, 14, 14, 3),
            Direction.EAST, box(0, 2, 2, 3, 14, 14),
            Direction.WEST, box(13, 2, 2, 16, 14, 14));

    public CircleStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return ChalkPatternBlock.supported(level, pos, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /** Placed some other way than by hand: take up the signal already there. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        super.onPlace(state, level, pos, old, movedByPiston);
        updatePower(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, orientation, movedByPiston);
        updatePower(level, pos);
    }

    private void updatePower(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        BlockState current = level.getBlockState(pos);
        boolean powered = level.hasNeighborSignal(pos);
        if (current.is(this) && current.getValue(POWERED) != powered) {
            level.setBlock(pos, current.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CircleStoneBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ThaumoryBlocks.CIRCLE_STONE_ENTITY.get(), CircleStoneBlockEntity::serverTick);
    }
}
