package one.nxeu.thaumory.circle.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.block.core.CircleCoreBlock;
import one.nxeu.thaumory.circle.CirclePlane;

/**
 * Bellum + Terra, triggered. Each activation digs out one layer behind the face the circle is drawn
 * on, a square as wide as the range, working deeper each time; the layer right behind the face is
 * left to hold the circle up. It takes what an iron pickaxe could and brings the drops to the Core
 * (requirements §17.4).
 */
final class MiningEffect implements CircleEffect {
    private static final String DEPTH = "depth";
    /** The first layer dug: the one behind the circle's own support. */
    private static final int FIRST_DEPTH = 2;
    /** How many layers one activation looks through for something to dig. */
    private static final int SEARCH = 16;

    @Override
    public boolean canApply(CircleContext context) {
        return nextLayer(context).isPresent();
    }

    @Override
    public void apply(CircleContext context) {
        Optional<Integer> depth = nextLayer(context);
        if (depth.isEmpty()) {
            return;
        }
        ServerLevel level = context.level();
        List<ItemStack> drops = new ArrayList<>();
        // What the drops are worked out with; made here, since items cannot be made before their components are.
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        for (BlockPos pos : layer(context, depth.get())) {
            BlockState state = level.getBlockState(pos);
            if (diggable(level, pos, state)) {
                for (ItemStack drop : Block.getDrops(state, level, pos, null, null, tool)) {
                    merge(drops, drop);
                }
                level.destroyBlock(pos, false);
                context.showAffected(pos);
            }
        }
        context.data().putInt(DEPTH, depth.get() + 1);
        Vec3 at = Vec3.atCenterOf(context.core());
        for (ItemStack drop : drops) {
            level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, drop, 0, 0, 0));
        }
    }

    /** The depth of the first layer from where the circle got to that has something to dig. */
    private static Optional<Integer> nextLayer(CircleContext context) {
        ServerLevel level = context.level();
        int from = Math.max(FIRST_DEPTH, context.data().getIntOr(DEPTH, FIRST_DEPTH));
        for (int depth = from; depth < from + SEARCH; depth++) {
            List<BlockPos> layer = layer(context, depth);
            if (layer.isEmpty()) {
                return Optional.empty();
            }
            for (BlockPos pos : layer) {
                if (diggable(level, pos, level.getBlockState(pos))) {
                    return Optional.of(depth);
                }
            }
        }
        return Optional.empty();
    }

    /** The square {@code depth} blocks behind the circle's face, or nothing past the world's edge. */
    private static List<BlockPos> layer(CircleContext context, int depth) {
        Direction front = context.level().getBlockState(context.core()).getOptionalValue(CircleCoreBlock.FACING).orElse(Direction.UP);
        BlockPos centre = context.core().relative(front.getOpposite(), depth);
        if (context.level().isOutsideBuildHeight(centre)) {
            return List.of();
        }
        int radius = CircleRange.blocks(context);
        List<BlockPos> cells = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = centre.offset(CirclePlane.offset(front, dx, dz));
                if (!context.level().isOutsideBuildHeight(pos)) {
                    cells.add(pos);
                }
            }
        }
        return cells;
    }

    /** Solid enough to dig, breakable, within an iron pickaxe's reach, and holding nothing. */
    private static boolean diggable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0 && !state.is(BlockTags.INCORRECT_FOR_IRON_TOOL);
    }

    private static void merge(List<ItemStack> into, ItemStack drop) {
        for (ItemStack stack : into) {
            if (ItemStack.isSameItemSameComponents(stack, drop) && stack.getCount() < stack.getMaxStackSize()) {
                int moved = Math.min(drop.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved);
                drop.shrink(moved);
                if (drop.isEmpty()) {
                    return;
                }
            }
        }
        if (!drop.isEmpty()) {
            into.add(drop.copy());
        }
    }
}
