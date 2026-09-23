package one.nxeu.thaumory.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Opens the book of everything the player knows. The screen lives on the client, which already has the knowledge. */
public final class ArcaneCodexItem extends Item {
    /** Set by the client on startup; does nothing on a dedicated server. */
    private static volatile Runnable openScreen = () -> {};

    public ArcaneCodexItem(Properties properties) {
        super(properties);
    }

    public static void setScreenOpener(Runnable opener) {
        openScreen = opener;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreen.run();
        }
        return InteractionResult.SUCCESS;
    }
}
