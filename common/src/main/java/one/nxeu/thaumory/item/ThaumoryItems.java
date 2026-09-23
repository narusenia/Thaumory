package one.nxeu.thaumory.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.ThaumoryBlocks;

public final class ThaumoryItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumory.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<ArcaneLoupeItem> ARCANE_LOUPE =
            register("arcane_loupe", ArcaneLoupeItem::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<BlockItem> CRUCIBLE =
            register("crucible", properties -> new BlockItem(ThaumoryBlocks.CRUCIBLE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    private ThaumoryItems() {}

    public static void register() {
        ITEMS.register();
        CreativeTabRegistry.append(CreativeTabRegistry.defer(CreativeModeTabs.TOOLS_AND_UTILITIES), ARCANE_LOUPE);
        CreativeTabRegistry.append(CreativeTabRegistry.defer(CreativeModeTabs.FUNCTIONAL_BLOCKS), CRUCIBLE);
    }

    private static <I extends Item> RegistrySupplier<I> register(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Thaumory.id(name));
        return ITEMS.register(name, () -> factory.apply(properties.setId(key)));
    }
}
