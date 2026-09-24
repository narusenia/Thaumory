package one.nxeu.thaumory.block.chalk;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.circle.CirclePlane;

/**
 * A pattern drawn in chalk on a face of a block (the floor, a wall or the ceiling): the plain line
 * or one of the modifiers. {@link #FACING} is the circle's front, pointing away from that face.
 * Patterns of any kind next to each other on the same face join up (any block in {@link #PATTERNS}
 * does); the connections are named by the floor's directions within the face ({@link CirclePlane}).
 * Drawn and erased with a chalk item.
 */
public final class ChalkPatternBlock extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH,
            Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.WEST, BlockStateProperties.WEST);
    /** Blocks that count as circle patterns and join up with chalk; addons put their own patterns here. */
    public static final TagKey<Block> PATTERNS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("thaumory", "circle_patterns"));
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, box(0, 0, 0, 16, 1, 16),
            Direction.DOWN, box(0, 15, 0, 16, 16, 16),
            Direction.NORTH, box(0, 0, 15, 16, 16, 16),
            Direction.SOUTH, box(0, 0, 0, 16, 16, 1),
            Direction.EAST, box(0, 0, 0, 1, 16, 16),
            Direction.WEST, box(15, 0, 0, 16, 16, 16));

    private final Supplier<? extends Item> chalk;

    /** @param chalk the chalk that draws this pattern, given back by pick block */
    public ChalkPatternBlock(Supplier<? extends Item> chalk, Properties properties) {
        super(properties);
        this.chalk = chalk;
        BlockState state = stateDefinition.any().setValue(FACING, Direction.UP);
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    /** This pattern at {@code pos} on the face that {@code front} points away from, joined to the patterns around it. */
    public BlockState connectedState(BlockGetter level, BlockPos pos, Direction front) {
        BlockState state = defaultBlockState().setValue(FACING, front);
        for (Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet()) {
            BlockState neighbor = level.getBlockState(pos.relative(CirclePlane.toWorld(front, connection.getKey())));
            state = state.setValue(connection.getValue(), joins(neighbor, front));
        }
        return state;
    }

    /** Patterns join only on the same face; patterns without a front of their own lie on the floor. */
    private static boolean joins(BlockState neighbor, Direction front) {
        return neighbor.is(PATTERNS) && front(neighbor) == front;
    }

    /** The front of a pattern or Core: up for one that does not say. */
    public static Direction front(BlockState state) {
        return state.getOptionalValue(FACING).orElse(Direction.UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connectedState(context.getLevel(), context.getClickedPos(), context.getClickedFace());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        Direction front = state.getValue(FACING);
        if (direction == front.getOpposite() && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        for (Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet()) {
            if (CirclePlane.toWorld(front, connection.getKey()) == direction) {
                return state.setValue(connection.getValue(), joins(neighbor, front));
            }
        }
        return state;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return supported(level, pos, state.getValue(FACING));
    }

    /** Whether the block behind {@code pos} has a sturdy face towards {@code front} to draw on. */
    public static boolean supported(LevelReader level, BlockPos pos, Direction front) {
        BlockPos behind = pos.relative(front.getOpposite());
        return level.getBlockState(behind).isFaceSturdy(level, behind, front);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(chalk.get());
    }
}
