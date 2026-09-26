package one.nxeu.thaumory.wand;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * The focus menu's arithmetic (requirements §17.7): which inventory slots it offers, and which entry
 * the mouse points at. Entries go clockwise from the top.
 */
public final class FocusMenu {
    private FocusMenu() {}

    /**
     * The inventory slots whose foci the menu offers: the first of each kind, in slot order, leaving out
     * the kind the wand already carries.
     *
     * @param items the item in each inventory slot, or null where it is empty
     */
    public static List<Integer> slots(List<Identifier> items, Set<Identifier> foci, Optional<Identifier> current) {
        List<Integer> slots = new ArrayList<>();
        Set<Identifier> seen = new HashSet<>();
        current.ifPresent(seen::add);
        for (int slot = 0; slot < items.size(); slot++) {
            Identifier item = items.get(slot);
            if (item != null && foci.contains(item) && seen.add(item)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    /** Where entry {@code index} of {@code count} sits: radians clockwise from the top. */
    public static double angle(int index, int count) {
        return 2 * Math.PI * index / count;
    }

    /**
     * The entry the mouse points at, {@code (dx, dy)} from the centre with y going down the screen, or
     * none while it stays within {@code deadZone} of the centre.
     */
    public static OptionalInt pick(int count, double dx, double dy, double deadZone) {
        if (count <= 0 || dx * dx + dy * dy <= deadZone * deadZone) {
            return OptionalInt.empty();
        }
        double clockwise = Math.atan2(dx, -dy);
        if (clockwise < 0) {
            clockwise += 2 * Math.PI;
        }
        double sector = 2 * Math.PI / count;
        return OptionalInt.of((int) Math.floor((clockwise + sector / 2) / sector) % count);
    }
}
