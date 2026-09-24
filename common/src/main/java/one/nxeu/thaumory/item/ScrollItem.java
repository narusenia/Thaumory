package one.nxeu.thaumory.item;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.infusion.Infusion;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.infusion.Infusions;

/**
 * A blank scroll after infusion (requirements §10.3): right-click to set its one effect off once, for
 * free. It is used up only when the effect comes to something.
 */
public final class ScrollItem extends Item {
    public ScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!cast(server, player, stack)) {
            player.sendOverlayMessage(Component.translatable("message.thaumory.scroll.nothing"));
            server.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.2f);
            return InteractionResult.FAIL;
        }
        server.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    /** Sets the scroll's effect off for {@code player} and uses one up; false, keeping it, when nothing came of it. */
    public static boolean cast(ServerLevel level, Player player, ItemStack stack) {
        Optional<Infusion> infusion = stack.getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY).list().stream().findFirst();
        Optional<InfusionEffect> effect = infusion.flatMap(i -> ThaumoryApi.infusionEffects().get(i.effect())).filter(InfusionEffect::castable);
        if (effect.isEmpty() || !effect.get().cast(InfusionRuntime.context(level, player, stack, infusion.get()))) {
            return false;
        }
        stack.consume(1, player);
        return true;
    }
}
