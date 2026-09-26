package one.nxeu.thaumory.pouch;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * An Essentia pouch's six jar slots above the player's inventory (requirements §17.6). On the server
 * the slots write straight back into the pouch, and the pouch's own slot is locked while it is open.
 */
public final class PouchMenu extends AbstractContainerMenu {
    private static final int POUCH_SLOTS = EssentiaPouchItem.SLOTS;

    private final Container jars;
    private final Inventory inventory;
    /** The inventory slot holding the pouch, or -1 on the client. */
    private final int pouchSlot;

    /** The client's copy, filled in by the server. */
    public PouchMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(POUCH_SLOTS), -1);
    }

    PouchMenu(int id, Inventory inventory, int pouchSlot) {
        this(id, inventory, backedBy(inventory, pouchSlot), pouchSlot);
    }

    private PouchMenu(int id, Inventory inventory, Container jars, int pouchSlot) {
        super(ThaumoryMenus.ESSENTIA_POUCH.get(), id);
        this.jars = jars;
        this.inventory = inventory;
        this.pouchSlot = pouchSlot;
        for (int i = 0; i < POUCH_SLOTS; i++) {
            addSlot(new Slot(jars, i, 35 + i * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(ThaumoryItems.JAR.get());
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(playerSlot(9 + row * 9 + column, 8 + column * 18, 51 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(playerSlot(column, 8 + column * 18, 109));
        }
    }

    /** A container over the pouch's jars that writes every change back into the pouch. */
    private static Container backedBy(Inventory inventory, int pouchSlot) {
        SimpleContainer container = new SimpleContainer(POUCH_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                ItemStack pouch = inventory.getItem(pouchSlot);
                if (pouch.is(ThaumoryItems.ESSENTIA_POUCH.get())) {
                    EssentiaPouchItem.setJars(pouch, getItems());
                }
            }
        };
        var held = EssentiaPouchItem.jars(inventory.getItem(pouchSlot));
        for (int i = 0; i < POUCH_SLOTS; i++) {
            container.getItems().set(i, held.get(i));
        }
        return container;
    }

    /** A slot of the player's inventory; the one holding the open pouch cannot be touched. */
    private Slot playerSlot(int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != pouchSlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return index != pouchSlot;
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return pouchSlot < 0 || inventory.getItem(pouchSlot).is(ThaumoryItems.ESSENTIA_POUCH.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < POUCH_SLOTS
                ? moveItemStackTo(stack, POUCH_SLOTS, slots.size(), true)
                : stack.is(ThaumoryItems.JAR.get()) && moveItemStackTo(stack, 0, POUCH_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
