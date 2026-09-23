package one.nxeu.thaumory.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
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
    public static final RegistrySupplier<ArcaneCodexItem> ARCANE_CODEX =
            register("arcane_codex", ArcaneCodexItem::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<Item> BLANK_RUNE = register("blank_rune", Item::new, new Item.Properties());
    public static final RegistrySupplier<Item> CHALK = chalk("chalk");
    public static final RegistrySupplier<Item> AMPLIFYING_CHALK = chalk("amplifying_chalk");
    public static final RegistrySupplier<Item> EXTENDING_CHALK = chalk("extending_chalk");
    public static final RegistrySupplier<Item> ECONOMIZING_CHALK = chalk("economizing_chalk");
    public static final RegistrySupplier<Item> STABILIZING_CHALK = chalk("stabilizing_chalk");
    public static final RegistrySupplier<BlockItem> CRUCIBLE =
            register("crucible", properties -> new BlockItem(ThaumoryBlocks.CRUCIBLE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    private ThaumoryItems() {}

    public static void register() {
        ITEMS.register();
        CreativeTabRegistry.append(CreativeTabRegistry.defer(CreativeModeTabs.TOOLS_AND_UTILITIES), ARCANE_LOUPE);
        CreativeTabRegistry.append(CreativeTabRegistry.defer(CreativeModeTabs.TOOLS_AND_UTILITIES), ARCANE_CODEX);
        for (RegistrySupplier<Item> item : List.of(CHALK, AMPLIFYING_CHALK, EXTENDING_CHALK, ECONOMIZING_CHALK, STABILIZING_CHALK)) {
            CreativeTabRegistry.append(CreativeTabRegistry.defer(CreativeModeTabs.TOOLS_AND_UTILITIES), item);
        }
        CreativeTabRegistry.append(CreativeTabRegistry.defer(CreativeModeTabs.INGREDIENTS), BLANK_RUNE);
        CreativeTabRegistry.append(CreativeTabRegistry.defer(CreativeModeTabs.FUNCTIONAL_BLOCKS), CRUCIBLE);
    }

    /** Draws lines for magic circles (M1-17); each block drawn costs one durability. */
    private static RegistrySupplier<Item> chalk(String name) {
        return register(name, Item::new, new Item.Properties().durability(64));
    }

    private static <I extends Item> RegistrySupplier<I> register(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Thaumory.id(name));
        return ITEMS.register(name, () -> factory.apply(properties.setId(key)));
    }
}
