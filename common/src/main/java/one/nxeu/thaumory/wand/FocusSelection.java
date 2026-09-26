package one.nxeu.thaumory.wand;

import dev.architectury.networking.NetworkManager;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.network.SelectFocusPayload;

/**
 * Putting foci on and off the wand in the main hand, as the focus menu asks (requirements §17.7). The
 * menu offers the slots of {@link Inventory#getNonEquipmentItems()}.
 */
public final class FocusSelection {
    private FocusSelection() {}

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SelectFocusPayload.TYPE, SelectFocusPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        select(player, payload.slot());
                    }
                }));
    }

    /**
     * Puts the focus in {@code slot} on the wand, the wand's old focus going back into that slot, or
     * with {@link SelectFocusPayload#DETACH} takes the focus off into the inventory (dropped if full).
     *
     * @return whether the wand changed
     */
    public static boolean select(ServerPlayer player, int slot) {
        ItemStack wand = player.getMainHandItem();
        if (!wand.is(ThaumoryItems.WAND.get())) {
            return false;
        }
        Optional<ItemStack> old = WandCasting.focusItem(wand).flatMap(BuiltInRegistries.ITEM::getOptional).map(ItemStack::new);
        Inventory inventory = player.getInventory();
        if (slot == SelectFocusPayload.DETACH) {
            if (old.isEmpty()) {
                return false;
            }
            wand.remove(ThaumoryComponents.WAND_FOCUS.get());
            inventory.placeItemBackInInventory(old.get(), Prediction.SERVER_ONLY);
        } else {
            if (slot < 0 || slot >= inventory.getNonEquipmentItems().size()) {
                return false;
            }
            ItemStack chosen = inventory.getItem(slot);
            Optional<WandFocus> focus = WandFoci.of(chosen);
            Identifier item = BuiltInRegistries.ITEM.getKey(chosen.getItem());
            if (focus.isEmpty() || WandCasting.focusItem(wand).filter(item::equals).isPresent()) {
                return false;
            }
            wand.set(ThaumoryComponents.WAND_FOCUS.get(), item);
            if (chosen.getCount() > 1) {
                chosen.shrink(1);
                old.ifPresent(stack -> inventory.placeItemBackInInventory(stack, Prediction.SERVER_ONLY));
            } else {
                inventory.setItem(slot, old.orElse(ItemStack.EMPTY));
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
        return true;
    }
}
