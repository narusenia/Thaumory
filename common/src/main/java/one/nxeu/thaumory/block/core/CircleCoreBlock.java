package one.nxeu.thaumory.block.core;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * The centre of a magic circle, drawn flat on the ground; its rings are drawn by the client for each rune. A rune goes into the first empty slot with a right click; a
 * sneaking right click on an empty hand takes out the last one.
 *
 * <p>A pedestal can be built into it (requirements §10.1): the core then stands as a slim altar,
 * holding one item to infuse. Any item that does not stack goes on with a right click, except
 * Thaumory's own tools; an empty hand takes it back.
 */
public final class CircleCoreBlock extends BaseEntityBlock {
    public static final BooleanProperty PEDESTAL = BooleanProperty.create("pedestal");
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape ALTAR = Shapes.or(box(3, 0, 3, 13, 2, 13), box(6, 2, 6, 10, 10, 10), box(4, 10, 4, 12, 12, 12));

    public CircleCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PEDESTAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PEDESTAL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PEDESTAL) ? ALTAR : SHAPE;
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
        if (stack.is(ThaumoryItems.PEDESTAL.get()) && !state.getValue(PEDESTAL)) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(PEDESTAL, true), Block.UPDATE_ALL);
                stack.consume(1, player);
                level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(PEDESTAL) && core.pedestalItem().isEmpty() && !stack.isStackable() && !stack.is(ThaumoryItems.PEDESTAL_IGNORED)) {
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
        if (core.runes().size() >= CircleCoreBlockEntity.SLOTS) {
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
