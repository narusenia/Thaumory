package one.nxeu.thaumory.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;

public final class ThaumoryItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumory.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Thaumory.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<ArcaneLoupeItem> ARCANE_LOUPE =
            register("arcane_loupe", ArcaneLoupeItem::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<WandItem> WAND =
            register("wand", WandItem::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<ArcaneCodexItem> ARCANE_CODEX =
            register("arcane_codex", ArcaneCodexItem::new, new Item.Properties().stacksTo(1));
    /** Made from the book only, so it is not in the creative tab. */
    public static final RegistrySupplier<TranscriptItem> TRANSCRIPT =
            register("transcript", TranscriptItem::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<Item> BLANK_RUNE = register("blank_rune", Item::new, new Item.Properties());
    public static final RegistrySupplier<RuneItem> RUNE = register("rune", RuneItem::new, new Item.Properties());
    public static final RegistrySupplier<ChalkItem> CHALK = chalk("chalk", () -> ThaumoryBlocks.CHALK_LINE.get());
    public static final RegistrySupplier<ChalkItem> AMPLIFYING_CHALK = chalk("amplifying_chalk", () -> ThaumoryBlocks.AMPLIFYING_PATTERN.get());
    public static final RegistrySupplier<ChalkItem> EXTENDING_CHALK = chalk("extending_chalk", () -> ThaumoryBlocks.EXTENDING_PATTERN.get());
    public static final RegistrySupplier<ChalkItem> ECONOMIZING_CHALK = chalk("economizing_chalk", () -> ThaumoryBlocks.ECONOMIZING_PATTERN.get());
    public static final RegistrySupplier<ChalkItem> STABILIZING_CHALK = chalk("stabilizing_chalk", () -> ThaumoryBlocks.STABILIZING_PATTERN.get());
    public static final RegistrySupplier<JarItem> JAR =
            register("jar", properties -> new JarItem(ThaumoryBlocks.JAR.get(), properties),
                    new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());
    public static final RegistrySupplier<Item> LABEL = register("label", Item::new, new Item.Properties());
    public static final RegistrySupplier<BlockItem> CRUCIBLE =
            register("crucible", properties -> new BlockItem(ThaumoryBlocks.CRUCIBLE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    public static final RegistrySupplier<BlockItem> PIPE =
            register("pipe", properties -> new BlockItem(ThaumoryBlocks.PIPE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    public static final RegistrySupplier<BlockItem> FILTER_PIPE =
            register("filter_pipe", properties -> new BlockItem(ThaumoryBlocks.FILTER_PIPE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());
    public static final RegistrySupplier<BlockItem> VALVE =
            register("valve", properties -> new BlockItem(ThaumoryBlocks.VALVE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());
    public static final RegistrySupplier<BlockItem> PUMP =
            register("pump", properties -> new BlockItem(ThaumoryBlocks.PUMP.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    public static final RegistrySupplier<BlockItem> CORE =
            register("core", properties -> new BlockItem(ThaumoryBlocks.CORE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    public static final RegistrySupplier<BlockItem> POLLUTED_SOIL =
            register("polluted_soil", properties -> new BlockItem(ThaumoryBlocks.POLLUTED_SOIL.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());
    public static final RegistrySupplier<BlockItem> POLLUTED_STONE =
            register("polluted_stone", properties -> new BlockItem(ThaumoryBlocks.POLLUTED_STONE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    private ThaumoryItems() {}

    /** Every Thaumory item, in the order the creative tab shows them. Runes come once per aspect. */
    private static final List<RegistrySupplier<? extends Item>> TAB_ORDER = List.of(
            ARCANE_LOUPE, WAND, ARCANE_CODEX, CRUCIBLE, JAR, LABEL, PIPE, FILTER_PIPE, VALVE, PUMP, BLANK_RUNE, RUNE, CORE,
            CHALK, AMPLIFYING_CHALK, EXTENDING_CHALK, ECONOMIZING_CHALK, STABILIZING_CHALK, POLLUTED_SOIL, POLLUTED_STONE);

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("thaumory", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup.thaumory"))
            .icon(() -> new ItemStack(ARCANE_CODEX.get()))
            .displayItems((parameters, output) -> TAB_ORDER.forEach(item -> {
                if (item == RUNE) {
                    ThaumoryApi.aspects().all().forEach(aspect -> output.accept(RuneItem.of(aspect)));
                } else {
                    output.accept(item.get());
                }
            }))));

    public static void register() {
        ITEMS.register();
        TABS.register();
    }

    /** Draws a pattern for magic circles; each block drawn costs one durability. */
    private static RegistrySupplier<ChalkItem> chalk(String name, Supplier<ChalkPatternBlock> pattern) {
        return register(name, properties -> new ChalkItem(pattern, properties), new Item.Properties().durability(64));
    }

    private static <I extends Item> RegistrySupplier<I> register(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Thaumory.id(name));
        return ITEMS.register(name, () -> factory.apply(properties.setId(key)));
    }
}
