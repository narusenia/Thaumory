package one.nxeu.thaumory.pouch;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import one.nxeu.thaumory.Thaumory;

/** Thaumory's container menus. */
public final class ThaumoryMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Thaumory.MOD_ID, Registries.MENU);

    public static final RegistrySupplier<MenuType<PouchMenu>> ESSENTIA_POUCH = MENUS.register("essentia_pouch",
            () -> new MenuType<>(PouchMenu::new, FeatureFlags.VANILLA_SET));

    private ThaumoryMenus() {}

    public static void register() {
        MENUS.register();
    }
}
