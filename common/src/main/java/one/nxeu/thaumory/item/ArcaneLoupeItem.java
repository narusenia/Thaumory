package one.nxeu.thaumory.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.scan.ItemScanner;

/**
 * Scans what the player looks at. Right-clicking a block is caught earlier by
 * {@link ItemScanner#register()} so the block's own interaction never runs; this handles the rest.
 * Sneaking is left for measuring Flux (M1-24).
 */
public final class ArcaneLoupeItem extends Item {
    public ArcaneLoupeItem(Properties properties) {
        super(properties);
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
