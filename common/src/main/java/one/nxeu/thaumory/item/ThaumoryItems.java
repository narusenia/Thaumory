package one.nxeu.thaumory.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.block.ThaumoryBlocks;

public final class ThaumoryItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumory.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Thaumory.MOD_ID, Registries.CREATIVE_MODE_TAB);

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
    public static final RegistrySupplier<JarItem> JAR =
            register("jar", properties -> new JarItem(ThaumoryBlocks.JAR.get(), properties),
                    new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());
    public static final RegistrySupplier<Item> LABEL = register("label", Item::new, new Item.Properties());
    public static final RegistrySupplier<BlockItem> CRUCIBLE =
            register("crucible", properties -> new BlockItem(ThaumoryBlocks.CRUCIBLE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    private ThaumoryItems() {}

    /** Every Thaumory item, in the order the creative tab shows them. */
    private static final List<RegistrySupplier<? extends Item>> TAB_ORDER = List.of(
            ARCANE_LOUPE, ARCANE_CODEX, CRUCIBLE, JAR, LABEL, BLANK_RUNE,
            CHALK, AMPLIFYING_CHALK, EXTENDING_CHALK, ECONOMIZING_CHALK, STABILIZING_CHALK);

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("thaumory", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup.thaumory"))
            .icon(() -> new ItemStack(ARCANE_CODEX.get()))
            .displayItems((parameters, output) -> TAB_ORDER.forEach(item -> output.accept(item.get())))));

    public static void register() {
        ITEMS.register();
        TABS.register();
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
