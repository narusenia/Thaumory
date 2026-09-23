package one.nxeu.thaumory.block.pipe;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.pipe.PipeNetworks;
import org.jspecify.annotations.Nullable;

/**
 * Carries Essentia between the containers it touches (requirements §8.2). Joins every pipe next to
 * it and every block with an Essentia storage, except a Crucible, which needs a pump.
 */
public final class EssentiaPipeBlock extends BaseEntityBlock {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH,
            Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.WEST, BlockStateProperties.WEST,
            Direction.UP, BlockStateProperties.UP,
            Direction.DOWN, BlockStateProperties.DOWN);
    private static final VoxelShape CENTRE = box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, box(5, 5, 0, 11, 11, 5),
            Direction.SOUTH, box(5, 5, 11, 11, 11, 16),
            Direction.WEST, box(0, 5, 5, 5, 11, 11),
            Direction.EAST, box(11, 5, 5, 16, 11, 11),
            Direction.DOWN, box(5, 0, 5, 11, 5, 11),
            Direction.UP, box(5, 11, 5, 11, 16, 11));

    public EssentiaPipeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTIONS.values().toArray(BooleanProperty[]::new));
    }

    /** Whether a pipe at {@code pos} joins what is on its {@code side}. */
    public static boolean joins(LevelReader level, BlockPos pos, Direction side) {
        BlockPos neighbor = pos.relative(side);
        if (level.getBlockState(neighbor).getBlock() instanceof EssentiaPipeBlock) {
            return true;
        }
        if (!(level instanceof Level world) || level.getBlockEntity(neighbor) instanceof CrucibleBlockEntity) {
            return false;
        }
        return Thaumory.essentia().find(world, neighbor, side.getOpposite()).isPresent();
    }

    private BlockState connectedState(LevelReader level, BlockPos pos, BlockState state) {
        for (Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet()) {
            state = state.setValue(connection.getValue(), joins(level, pos, connection.getKey()));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectedState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return state.setValue(CONNECTIONS.get(direction), joins(level, pos, direction));
    }

    /** Placed some other way than by hand (a command, a structure): join up with what is already there. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        if (!level.isClientSide()) {
            BlockState connected = connectedState(level, pos, state);
            if (connected != state) {
                level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** A block entity next door may come or go without the block changing, so look again and rebuild. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level instanceof ServerLevel server) {
            BlockState connected = connectedState(level, pos, state);
            if (connected != state) {
                level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
            }
            PipeNetworks.of(server).invalidate(pos);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTRE;
        for (Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet()) {
            if (state.getValue(connection.getValue())) {
                shape = Shapes.or(shape, ARMS.get(connection.getKey()));
            }
        }
        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }
}
