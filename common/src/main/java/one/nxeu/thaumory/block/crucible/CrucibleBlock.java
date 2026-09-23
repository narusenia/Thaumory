package one.nxeu.thaumory.block.crucible;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.crucible.CrucibleTank;

/**
 * Melts items into Essentia. The block entity holds the contents; the block state mirrors the
 * water level (for the model) and whether it is boiling (for particles).
 */
public final class CrucibleBlock extends BaseEntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, CrucibleTank.MAX_WATER);
    public static final BooleanProperty BOILING = BooleanProperty.create("boiling");

    /** Same outline as a cauldron: open on top, with legs. */
    static final VoxelShape INSIDE = box(2, 4, 2, 14, 16, 14);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(),
            Shapes.or(box(0, 0, 4, 16, 3, 12), box(4, 0, 0, 12, 3, 16), box(2, 0, 2, 14, 3, 14), INSIDE),
            BooleanOp.ONLY_FIRST);

    public CrucibleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(BOILING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, BOILING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrucibleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ThaumoryBlocks.CRUCIBLE_ENTITY.get(), CrucibleBlockEntity::serverTick);
    }

    /** Water goes in from a bucket (full) or a water bottle (one level). It cannot be taken back out. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.WATER_BUCKET)) {
            return fill(state, level, pos, player, hand, stack, CrucibleTank.MAX_WATER, new ItemStack(Items.BUCKET), SoundEvents.BUCKET_EMPTY);
        }
        if (stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) {
            return fill(state, level, pos, player, hand, stack, state.getValue(LEVEL) + 1, new ItemStack(Items.GLASS_BOTTLE), SoundEvents.BOTTLE_EMPTY);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static InteractionResult fill(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            ItemStack stack, int water, ItemStack emptied, SoundEvent sound) {
        if (state.getValue(LEVEL) >= CrucibleTank.MAX_WATER) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            crucible.setWater(water);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, emptied));
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BOILING)) {
            return;
        }
        double surface = pos.getY() + (6.0 + 3.0 * state.getValue(LEVEL)) / 16.0;
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, surface, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0, 0.02, 0);
        }
        if (random.nextInt(12) == 0) {
            level.playLocalSound(pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.3f, 1.2f, false);
        }
    }
}
