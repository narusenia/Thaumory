package one.nxeu.thaumory.block.chalk;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A pattern drawn in chalk on top of a block: the plain line or one of the modifiers. Patterns of
 * any kind next to each other on the same level join up. Drawn and erased with a chalk item.
 */
public final class ChalkPatternBlock extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH,
            Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.WEST, BlockStateProperties.WEST);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    private final Supplier<? extends Item> chalk;

    /** @param chalk the chalk that draws this pattern, given back by pick block */
    public ChalkPatternBlock(Supplier<? extends Item> chalk, Properties properties) {
        super(properties);
        this.chalk = chalk;
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    /** This pattern at {@code pos}, joined to the patterns around it. */
    public BlockState connectedState(BlockGetter level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet()) {
            state = state.setValue(connection.getValue(), joins(level.getBlockState(pos.relative(connection.getKey()))));
        }
        return state;
    }

    private static boolean joins(BlockState neighbor) {
        return neighbor.getBlock() instanceof ChalkPatternBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectedState(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        BooleanProperty connection = CONNECTIONS.get(direction);
        return connection == null ? state : state.setValue(connection, joins(neighbor));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(chalk.get());
    }
}
