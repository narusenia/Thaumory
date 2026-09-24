package one.nxeu.thaumory.circle.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;

/**
 * Mining used from an item or a scroll: digs into the face the user is looking at, a square 1 + 2 ×
 * level blocks across and {@code depth} deep, taking what the mining circle would. The drops land at
 * the user's feet. Nothing is paid with nothing to dig (requirements §10.2).
 */
final class MiningInfusion implements InfusionEffect {
    /** How far a user that is not a player reaches. */
    private static final double MOB_REACH = 4.5;

    @Override
    public boolean active() {
        return true;
    }

    @Override
    public boolean canUse(InfusionContext context) {
        return !blocks(context).isEmpty();
    }

    @Override
    public void use(InfusionContext context) {
        ServerLevel level = context.level();
        LivingEntity user = context.wearer();
        // What the drops are worked out with; made here, since items cannot be made before their components are.
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        for (BlockPos pos : blocks(context)) {
            BlockState state = level.getBlockState(pos);
            for (ItemStack drop : Block.getDrops(state, level, pos, null, user, tool)) {
                level.addFreshEntity(new ItemEntity(level, user.getX(), user.getY(), user.getZ(), drop, 0, 0, 0));
            }
            level.destroyBlock(pos, false, user);
        }
    }

    /** What there is to dig behind the face the user looks at, or nothing when they look at no block. */
    private static List<BlockPos> blocks(InfusionContext context) {
        LivingEntity user = context.wearer();
        double reach = user instanceof Player player ? player.blockInteractionRange() : MOB_REACH;
        HitResult hit = user.pick(reach, 1, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return List.of();
        }
        ServerLevel level = context.level();
        Direction into = blockHit.getDirection().getOpposite();
        Direction[] across = across(into.getAxis());
        int half = context.infusionLevel();
        int depth = Math.max(1, (int) Math.round(context.setting("depth", 3)));
        Optional<ServerPlayer> player = user instanceof ServerPlayer p ? Optional.of(p) : Optional.empty();
        List<BlockPos> blocks = new ArrayList<>();
        for (int d = 0; d < depth; d++) {
            for (int a = -half; a <= half; a++) {
                for (int b = -half; b <= half; b++) {
                    BlockPos pos = blockHit.getBlockPos().relative(into, d).relative(across[0], a).relative(across[1], b);
                    if (MiningEffect.diggable(level, pos, level.getBlockState(pos)) && player.map(p -> mayBreak(p, level, pos)).orElse(true)) {
                        blocks.add(pos);
                    }
                }
            }
        }
        return blocks;
    }

    /** Spawn protection and adventure mode apply as they would to breaking the block by hand. */
    private static boolean mayBreak(ServerPlayer player, ServerLevel level, BlockPos pos) {
        return level.mayInteract(player, pos) && !player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer());
    }

    /** Two directions across a face lying on {@code axis}. */
    private static Direction[] across(Direction.Axis axis) {
        return switch (axis) {
            case Y -> new Direction[] {Direction.EAST, Direction.SOUTH};
            case X -> new Direction[] {Direction.UP, Direction.SOUTH};
            case Z -> new Direction[] {Direction.UP, Direction.EAST};
        };
    }
}
