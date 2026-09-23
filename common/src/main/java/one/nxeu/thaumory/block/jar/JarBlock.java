package one.nxeu.thaumory.block.jar;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.jar.JarContents;

/**
 * Stores Essentia. A label goes on with a right click while the jar holds one aspect, and comes
 * off with a sneaking right click on an empty hand. Filling and emptying is done by the jar item.
 */
public final class JarBlock extends BaseEntityBlock {
    public static final BooleanProperty LABELED = BooleanProperty.create("labeled");
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 14, 13);

    public JarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LABELED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LABELED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new JarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ThaumoryItems.LABEL.get()) || !(level.getBlockEntity(pos) instanceof JarBlockEntity jar)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        Optional<JarContents> labeled = jar.contents().labeled();
        if (labeled.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.thaumory.jar.cannot_label"));
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            jar.setContents(labeled.get());
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !state.getValue(LABELED) || !(level.getBlockEntity(pos) instanceof JarBlockEntity jar)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            jar.setContents(jar.contents().unlabeled());
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0f, 0.7f);
        }
        return InteractionResult.SUCCESS;
    }
}
