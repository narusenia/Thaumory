package one.nxeu.thaumory.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.flux.FluxReadings;
import one.nxeu.thaumory.scan.ItemScanner;

/**
 * Scans what the player looks at. Right-clicking a block is caught earlier by
 * {@link ItemScanner#register()} so the block's own interaction never runs; this handles the rest.
 * Sneaking is left free. Held in either hand, it also reads the chunk's Flux ({@link FluxReadings}).
 */
public final class ArcaneLoupeItem extends Item {
    public ArcaneLoupeItem(Properties properties) {
        super(properties);
    }

    public static boolean isHeldBy(Player player) {
        return player.getMainHandItem().is(ThaumoryItems.ARCANE_LOUPE.get()) || player.getOffhandItem().is(ThaumoryItems.ARCANE_LOUPE.get());
    }

    /**
     * Whether {@code player} sees the Flux and what containers, pipes and circles hold: holding the
     * loupe in either hand, or wearing the monocle. Other blocks' names and aspects need the loupe in hand.
     */
    public static boolean sees(Player player) {
        return isHeldBy(player) || player.getItemBySlot(EquipmentSlot.HEAD).is(ThaumoryItems.MONOCLE.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ItemScanner.scan(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
