package one.nxeu.thaumory.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.pouch.PouchMenu;

/** An Essentia pouch's six jar slots over the player's inventory (requirements §17.6). */
final class PouchScreen extends AbstractContainerScreen<PouchMenu> {
    private static final Identifier BACKGROUND = Thaumory.id("textures/gui/container/essentia_pouch.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 133;

    PouchScreen(PouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        inventoryLabelY = HEIGHT - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, WIDTH, HEIGHT, 256, 256);
    }
}
