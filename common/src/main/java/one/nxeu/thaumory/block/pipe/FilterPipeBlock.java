package one.nxeu.thaumory.block.pipe;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * A pipe that lets only one aspect in and out of the containers it touches (requirements §8.2).
 * A rune sets the aspect and is not used up; a sneaking empty hand takes the filter off.
 */
public final class FilterPipeBlock extends EssentiaPipeBlock {
    public FilterPipeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ThaumoryItems.RUNE.get()) || !(level.getBlockEntity(pos) instanceof PipeBlockEntity pipe)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        Optional<Aspect> aspect = RuneItem.aspect(stack);
        if (aspect.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            pipe.setFilter(aspect);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
            player.sendOverlayMessage(Component.translatable("message.thaumory.filter_pipe.set",
                    AspectText.name(aspect.get(), knows(player, aspect.get()))));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) || pipe.filter().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            pipe.setFilter(Optional.empty());
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
            player.sendOverlayMessage(Component.translatable("message.thaumory.filter_pipe.cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean knows(Player player, Aspect aspect) {
        return player instanceof ServerPlayer server && Thaumory.knowledge().get(server).knowsAspect(aspect.id());
    }
}
