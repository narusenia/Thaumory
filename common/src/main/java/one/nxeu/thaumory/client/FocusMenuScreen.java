package one.nxeu.thaumory.client;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.network.SelectFocusPayload;
import one.nxeu.thaumory.wand.FocusMenu;

/**
 * The focus menu (requirements §17.7): open while the focus key is held, with the foci the player
 * carries round a circle and "take off" last. Letting go of the key picks the entry the mouse points
 * towards; letting go with the mouse still in the middle changes nothing.
 */
final class FocusMenuScreen extends Screen {
    private static final int RADIUS = 52;
    private static final int DEAD_ZONE = 18;
    private static final int CELL = 22;

    /** One choice: the focus in an inventory slot, or taking the focus off. */
    private record Entry(ItemStack icon, Component name, int slot) {}

    private final KeyMapping key;
    private final ItemStack current;
    private final List<Entry> entries;
    private OptionalInt picked = OptionalInt.empty();

    private FocusMenuScreen(KeyMapping key, ItemStack current, List<Entry> entries) {
        super(Component.translatable("screen.thaumory.focus_menu"));
        this.key = key;
        this.current = current;
        this.entries = entries;
    }

    /**
     * The menu for the wand in {@code player}'s main hand, or none when there is nothing to choose: no
     * focus in the inventory and none on the wand.
     */
    static Optional<FocusMenuScreen> open(KeyMapping key, LocalPlayer player) {
        ItemStack wand = player.getMainHandItem();
        Optional<Identifier> focus = Optional.ofNullable(wand.get(ThaumoryComponents.WAND_FOCUS.get()));
        List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
        List<Identifier> items = new ArrayList<>();
        for (ItemStack stack : inventory) {
            items.add(stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        List<Entry> entries = new ArrayList<>();
        for (int slot : FocusMenu.slots(items, ClientWandParts.foci().keySet(), focus)) {
            ItemStack stack = inventory.get(slot);
            entries.add(new Entry(stack.copyWithCount(1), stack.getHoverName(), slot));
        }
        ItemStack current = focus.flatMap(BuiltInRegistries.ITEM::getOptional).map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (!current.isEmpty()) {
            entries.add(new Entry(new ItemStack(Items.BARRIER), Component.translatable("screen.thaumory.focus_menu.detach"),
                    SelectFocusPayload.DETACH));
        }
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FocusMenuScreen(key, current, entries));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The world stays in view behind the menu.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        picked = FocusMenu.pick(entries.size(), mouseX - cx, mouseY - cy, DEAD_ZONE);
        cell(graphics, cx, cy, false);
        if (!current.isEmpty()) {
            graphics.item(current, cx - 8, cy - 8);
        }
        for (int i = 0; i < entries.size(); i++) {
            double angle = FocusMenu.angle(i, entries.size());
            int x = cx + (int) Math.round(Math.sin(angle) * RADIUS);
            int y = cy - (int) Math.round(Math.cos(angle) * RADIUS);
            boolean chosen = picked.isPresent() && picked.getAsInt() == i;
            cell(graphics, x, y, chosen);
            graphics.item(entries.get(i).icon(), x - 8, y - 8);
        }
        Component label = picked.isPresent() ? entries.get(picked.getAsInt()).name() : current.isEmpty() ? title : current.getHoverName();
        graphics.centeredText(font, label, cx, cy + RADIUS + CELL, 0xFFFFFFFF);
    }

    /** A square behind an icon centred at (x, y), lit when the mouse points at it. */
    private static void cell(GuiGraphicsExtractor graphics, int x, int y, boolean lit) {
        int half = CELL / 2;
        graphics.fill(x - half, y - half, x + half, y + half, lit ? 0xC0E8D8A0 : 0x90201828);
        graphics.outline(x - half, y - half, CELL, CELL, lit ? 0xFFFFF0C0 : 0xFF8070A0);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (key.matches(event)) {
            choose();
            return true;
        }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        choose();
        return true;
    }

    /** Sends the entry the mouse points at, if any, and closes. */
    private void choose() {
        if (picked.isPresent()) {
            NetworkManager.sendToServer(new SelectFocusPayload(entries.get(picked.getAsInt()).slot()));
        }
        onClose();
    }
}
