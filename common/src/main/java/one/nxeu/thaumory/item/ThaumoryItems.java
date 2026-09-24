package one.nxeu.thaumory.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
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
    /** Takes one infusion and turns into a {@link #SCROLL} (requirements §10.3). */
    public static final RegistrySupplier<Item> BLANK_SCROLL = register("blank_scroll", Item::new, new Item.Properties().stacksTo(16));
    /** Made from a blank scroll by infusion only, so it is not in the creative tab. */
    public static final RegistrySupplier<ScrollItem> SCROLL = register("scroll", ScrollItem::new, new Item.Properties().stacksTo(16));
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

    /** Built into a circle core rather than placed on its own (requirements §10.1). */
    public static final RegistrySupplier<Item> PEDESTAL = register("pedestal", Item::new, new Item.Properties());
    /** Thaumory's own tools, which keep working on a core with a pedestal instead of going onto it. */
    public static final TagKey<Item> PEDESTAL_IGNORED = TagKey.create(Registries.ITEM, Thaumory.id("pedestal_ignored"));

    public static final RegistrySupplier<BlockItem> CIRCLE_CORE =
            register("circle_core", properties -> new BlockItem(ThaumoryBlocks.CIRCLE_CORE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    public static final RegistrySupplier<BlockItem> POLLUTED_SOIL =
            register("polluted_soil", properties -> new BlockItem(ThaumoryBlocks.POLLUTED_SOIL.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());
    public static final RegistrySupplier<BlockItem> POLLUTED_STONE =
            register("polluted_stone", properties -> new BlockItem(ThaumoryBlocks.POLLUTED_STONE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    public static final EquipmentSet ARCANE_IRON = equipment("arcane_iron", ThaumoryMaterials.ARCANE_IRON_TOOL,
            ThaumoryMaterials.ARCANE_IRON_ARMOR, 6.0F, -3.1F, -2.0F, -1.0F);
    public static final EquipmentSet AETHER_SILVER = equipment("aether_silver", ThaumoryMaterials.AETHER_SILVER_TOOL,
            ThaumoryMaterials.AETHER_SILVER_ARMOR, 5.0F, -3.0F, -3.0F, 0.0F);

    private ThaumoryItems() {}

    /** Every Thaumory item, in the order the creative tab shows them. Runes come once per aspect. */
    private static final List<RegistrySupplier<? extends Item>> TAB_ORDER = List.of(
            ARCANE_LOUPE, WAND, ARCANE_CODEX, CRUCIBLE, JAR, LABEL, PIPE, FILTER_PIPE, VALVE, PUMP, BLANK_RUNE, RUNE, CIRCLE_CORE, PEDESTAL, BLANK_SCROLL,
            CHALK, AMPLIFYING_CHALK, EXTENDING_CHALK, ECONOMIZING_CHALK, STABILIZING_CHALK, POLLUTED_SOIL, POLLUTED_STONE);
    private static final List<RegistrySupplier<? extends Item>> TAB_ORDER_EQUIPMENT =
            Stream.concat(ARCANE_IRON.all().stream(), AETHER_SILVER.all().stream()).<RegistrySupplier<? extends Item>>map(item -> item).toList();

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("thaumory", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup.thaumory"))
            .icon(() -> new ItemStack(ARCANE_CODEX.get()))
            .displayItems((parameters, output) -> {
                TAB_ORDER.forEach(item -> {
                    if (item == RUNE) {
                        ThaumoryApi.aspects().all().forEach(aspect -> output.accept(RuneItem.of(aspect)));
                    } else {
                        output.accept(item.get());
                    }
                });
                TAB_ORDER_EQUIPMENT.forEach(item -> output.accept(item.get()));
            })));

    /** What an item becomes once infused: a blank scroll turns into a scroll, anything else stays itself. */
    public static ItemStack infusedForm(ItemStack stack) {
        return stack.is(BLANK_SCROLL.get()) ? stack.transmuteCopy(SCROLL.get()) : stack.copy();
    }

    public static void register() {
        ITEMS.register();
        TABS.register();
    }

    /** Draws a pattern for magic circles; each block drawn costs one durability. */
    private static RegistrySupplier<ChalkItem> chalk(String name, Supplier<ChalkPatternBlock> pattern) {
        return register(name, properties -> new ChalkItem(pattern, properties), new Item.Properties().durability(64));
    }

    /** An ingot and its gear. Swords, pickaxes and shovels share vanilla's baselines; axes and hoes differ per tier. */
    private static EquipmentSet equipment(String metal, ToolMaterial tool, ArmorMaterial armor,
            float axeDamage, float axeSpeed, float hoeDamage, float hoeSpeed) {
        return new EquipmentSet(
                register(metal + "_ingot", Item::new, new Item.Properties()),
                register(metal + "_sword", Item::new, new Item.Properties().sword(tool, 3.0F, -2.4F)),
                register(metal + "_pickaxe", Item::new, new Item.Properties().pickaxe(tool, 1.0F, -2.8F)),
                register(metal + "_axe", Item::new, new Item.Properties().axe(tool, axeDamage, axeSpeed)),
                register(metal + "_shovel", Item::new, new Item.Properties().shovel(tool, 1.5F, -3.0F)),
                register(metal + "_hoe", Item::new, new Item.Properties().hoe(tool, hoeDamage, hoeSpeed)),
                register(metal + "_helmet", Item::new, new Item.Properties().humanoidArmor(armor, ArmorType.HELMET)),
                register(metal + "_chestplate", Item::new, new Item.Properties().humanoidArmor(armor, ArmorType.CHESTPLATE)),
                register(metal + "_leggings", Item::new, new Item.Properties().humanoidArmor(armor, ArmorType.LEGGINGS)),
                register(metal + "_boots", Item::new, new Item.Properties().humanoidArmor(armor, ArmorType.BOOTS)));
    }

    private static <I extends Item> RegistrySupplier<I> register(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Thaumory.id(name));
        return ITEMS.register(name, () -> factory.apply(properties.setId(key)));
    }
}
