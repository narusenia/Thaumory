package one.nxeu.thaumory.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Where the loader registers the screens for our menu types. Vanilla's {@code MenuScreens.register}
 * is private and Architectury 22 has no wrapper, so each loader hands its own way in to {@link
 * ThaumoryClient#init}.
 */
public interface MenuScreenFactories {
    /** Makes the screen for one open menu. */
    @FunctionalInterface
    interface Factory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
        S create(M menu, Inventory inventory, Component title);
    }

    <M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> void register(MenuType<M> type, Factory<M, S> factory);
}
