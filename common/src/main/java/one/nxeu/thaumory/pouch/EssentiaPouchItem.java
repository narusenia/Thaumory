package one.nxeu.thaumory.pouch;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.item.ThaumoryComponents;

/**
 * Carries a few jars (requirements §17.6) and tops up what keeps Essentia from them ({@link
 * PouchRuntime}). Right-clicking it opens its slots.
 */
public final class EssentiaPouchItem extends Item {
    public static final int SLOTS = 6;

    public EssentiaPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server) {
            int slot = hand == InteractionHand.MAIN_HAND ? server.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
            server.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PouchMenu(id, inventory, slot),
                    player.getItemInHand(hand).getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }

    /** The pouch's jars, one per slot, empty where a slot is. */
    public static NonNullList<ItemStack> jars(ItemStack pouch) {
        NonNullList<ItemStack> jars = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        pouch.getOrDefault(ThaumoryComponents.POUCH_CONTENTS.get(), ItemContainerContents.EMPTY).copyInto(jars);
        return jars;
    }

    public static void setJars(ItemStack pouch, List<ItemStack> jars) {
        if (jars.stream().allMatch(ItemStack::isEmpty)) {
            pouch.remove(ThaumoryComponents.POUCH_CONTENTS.get());
        } else {
            pouch.set(ThaumoryComponents.POUCH_CONTENTS.get(), ItemContainerContents.fromItems(jars));
        }
    }
}
