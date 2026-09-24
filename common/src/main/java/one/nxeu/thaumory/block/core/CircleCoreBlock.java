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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * The centre of a magic circle, drawn flat on the ground; its rings are drawn by the client for each rune. A rune goes into the first empty slot with a right click; a
 * sneaking right click on an empty hand takes out the last one.
 */
public final class CircleCoreBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

    public CircleCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        Identifier aspect = stack.is(ThaumoryItems.RUNE.get()) ? stack.get(ThaumoryComponents.RUNE_ASPECT.get()) : null;
        if (aspect == null || !(level.getBlockEntity(pos) instanceof CircleCoreBlockEntity core)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
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
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof CircleCoreBlockEntity core) || core.runes().isEmpty()) {
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
