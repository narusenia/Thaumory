package one.nxeu.thaumory.block.core;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.client.ClientCapacities;
import one.nxeu.thaumory.infusion.InfusionCapacities;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * The centre of a magic circle, drawn flat on the floor, a wall or the ceiling ({@link #FACING} is
 * the circle's front, pointing away from that face); its rings are drawn by the client for each
 * rune. It needs a sturdy face to be drawn on and breaks when that goes. A rune goes into the
 * first empty slot with a right click; a sneaking right click on an empty hand takes out the last
 * one.
 *
 * <p>Its rank (requirements §4.6) comes from what it is made of and sets how many rings it reads
 * and how many rune slots it has.
 *
 * <p>A pedestal can be built into a Core on the floor (requirements §10.1): the core then stands as a slim altar,
 * holding one item to infuse. Any item that does not stack goes on with a right click, except
 * Thaumory's own tools; an empty hand takes it back.
 */
public final class CircleCoreBlock extends BaseEntityBlock {
    public static final BooleanProperty PEDESTAL = BooleanProperty.create("pedestal");
    public static final EnumProperty<Direction> FACING = ChalkPatternBlock.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.UP, box(0, 0, 0, 16, 1, 16),
            Direction.DOWN, box(0, 15, 0, 16, 16, 16),
            Direction.NORTH, box(0, 0, 15, 16, 16, 16),
            Direction.SOUTH, box(0, 0, 0, 16, 16, 1),
            Direction.EAST, box(0, 0, 0, 1, 16, 16),
            Direction.WEST, box(15, 0, 0, 16, 16, 16));
    private static final VoxelShape ALTAR = Shapes.or(box(3, 0, 3, 13, 2, 13), box(6, 2, 6, 10, 10, 10), box(4, 10, 4, 12, 12, 12));

    private final int rank;

    public CircleCoreBlock(int rank, Properties properties) {
        super(properties);
        this.rank = rank;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(PEDESTAL, false));
    }

    public int rank() {
        return rank;
    }

    /** Rings read: 3 at rank 1, one more each rank. */
    public int maxRings() {
        return 2 + rank;
    }

    /** Rune slots: 3, and a fourth from rank 3. */
    public int slots() {
        return rank >= 3 ? 4 : 3;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PEDESTAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PEDESTAL) ? ALTAR : SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CircleCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ThaumoryBlocks.CIRCLE_CORE_ENTITY.get(), CircleCoreBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CircleCoreBlockEntity core)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.is(ThaumoryItems.PEDESTAL.get()) && !state.getValue(PEDESTAL) && state.getValue(FACING) == Direction.UP) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(PEDESTAL, true), Block.UPDATE_ALL);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(PEDESTAL) && core.pedestalItem().isEmpty() && goesOnPedestal(level, stack)) {
            if (!level.isClientSide()) {
                core.setPedestalItem(stack.split(1));
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        Identifier aspect = stack.is(ThaumoryItems.RUNE.get()) ? stack.get(ThaumoryComponents.RUNE_ASPECT.get()) : null;
        if (aspect == null) {
            // An empty hand goes on to take the item off the pedestal; anything else (the wand, a jar) does its own work.
            return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
        }
        if (core.runes().size() >= core.slots()) {
            player.sendOverlayMessage(Component.translatable("message.thaumory.core.full"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            core.insert(aspect);
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Items that do not stack, and items with an infusion capacity (a stack of blank scrolls, say) or
     * blank circle stones, one at a time. The client goes by the capacities the server sent it.
     */
    private static boolean goesOnPedestal(Level level, ItemStack stack) {
        if (stack.isEmpty() || stack.is(ThaumoryItems.PEDESTAL_IGNORED)) {
            return false;
        }
        int capacity = level.isClientSide() ? ClientCapacities.of(stack.getItem()) : InfusionCapacities.of(stack.getItem());
        return !stack.isStackable() || capacity > 0 || stack.is(ThaumoryItems.BLANK_CIRCLE_STONE.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CircleCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown() && !core.pedestalItem().isEmpty() && player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide()) {
                player.getInventory().placeItemBackInInventory(core.takePedestalItem(), Prediction.SERVER_ONLY);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown() || core.runes().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Optional<Identifier> removed = core.removeLast();
            removed.ifPresent(aspect -> player.getInventory().placeItemBackInInventory(RuneItem.of(aspect), Prediction.SERVER_ONLY));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }
}
