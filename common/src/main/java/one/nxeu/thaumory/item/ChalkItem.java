package one.nxeu.thaumory.item;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;

/**
 * Draws its pattern on the clicked face of a block (the floor, a wall or the ceiling), one
 * durability per block. Right-clicking a pattern of the same kind erases it for free; one of
 * another kind is drawn over, on the same face.
 */
public final class ChalkItem extends Item {
    private final Supplier<? extends ChalkPatternBlock> pattern;

    public ChalkItem(Supplier<? extends ChalkPatternBlock> pattern, Properties properties) {
        super(properties);
        this.pattern = pattern;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ChalkPatternBlock drawn = pattern.get();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        if (clickedState.getBlock() instanceof ChalkPatternBlock existing) {
            if (existing == drawn) {
                if (!level.isClientSide()) {
                    level.removeBlock(clicked, false);
                    level.playSound(null, clicked, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 1.0f, 1.2f);
                }
                return InteractionResult.SUCCESS;
            }
            return draw(context, clicked, ChalkPatternBlock.front(clickedState));
        }

        // Grass and the like are drawn over on the ground they stand on.
        boolean replacing = clickedState.canBeReplaced();
        BlockPos target = replacing ? clicked : clicked.relative(context.getClickedFace());
        Direction front = replacing ? Direction.UP : context.getClickedFace();
        if (!level.getBlockState(target).canBeReplaced() || !ChalkPatternBlock.supported(level, target, front)) {
            return InteractionResult.FAIL;
        }
        return draw(context, target, front);
    }

    private InteractionResult draw(UseOnContext context, BlockPos pos, Direction front) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            level.setBlock(pos, pattern.get().connectedState(level, pos, front), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CALCITE_STEP, SoundSource.BLOCKS, 1.0f, 1.4f);
            Player player = context.getPlayer();
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand());
            }
        }
        return InteractionResult.SUCCESS;
    }
}
