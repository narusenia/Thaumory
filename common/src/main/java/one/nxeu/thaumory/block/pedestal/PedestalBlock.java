package one.nxeu.thaumory.block.pedestal;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * Stands over a Core and holds the item a circle is infused into (requirements §10.1). A right
 * click puts one item from the hand on it; an empty hand takes it back. The wand is left to do
 * its own work.
 */
public final class PedestalBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 2, 13), box(6, 2, 6, 10, 10, 10), box(4, 10, 4, 12, 12, 12));

    public PedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ThaumoryItems.WAND.get()) || !(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal)
                || !pedestal.item().isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            pedestal.setItem(stack.split(1));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || pedestal.item().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(pedestal.take(), Prediction.SERVER_ONLY);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }
}
