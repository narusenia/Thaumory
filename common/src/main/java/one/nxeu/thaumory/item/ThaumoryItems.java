package one.nxeu.thaumory.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.jar.JarSettings;

public final class ThaumoryItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Thaumory.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Thaumory.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<ArcaneLoupeItem> ARCANE_LOUPE =
            register("arcane_loupe", ArcaneLoupeItem::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<WandItem> WAND =
            register("wand", WandItem::new, new Item.Properties().stacksTo(1));
    /** Wand parts (requirements §7.1): caps set how much Essentia a wand holds, cores how strongly it casts. */
    public static final RegistrySupplier<Item> GOLD_WAND_CAP = register("gold_wand_cap", Item::new, new Item.Properties());
    public static final RegistrySupplier<Item> ARCANE_IRON_WAND_CAP = register("arcane_iron_wand_cap", Item::new, new Item.Properties());
    public static final RegistrySupplier<Item> AETHER_SILVER_WAND_CAP = register("aether_silver_wand_cap", Item::new, new Item.Properties());
    public static final RegistrySupplier<Item> CRYSTAL_WAND_CORE = register("crystal_wand_core", Item::new, new Item.Properties());
    /** Casts nothing; each focus is made from one (requirements §17.7). */
    public static final RegistrySupplier<Item> BLANK_FOCUS = register("blank_focus", Item::new, new Item.Properties().stacksTo(16));
    /** Goes onto a wand; what it casts comes from {@code thaumory/wand_focus} in the datapack. */
    public static final RegistrySupplier<Item> LIGHT_FOCUS = register("light_focus", Item::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<ArcaneCodexItem> ARCANE_CODEX =
            register("arcane_codex", ArcaneCodexItem::new, new Item.Properties().stacksTo(1));
    /** Made from the book only, so it is not in the creative tab. */
    public static final RegistrySupplier<TranscriptItem> TRANSCRIPT =
            register("transcript", TranscriptItem::new, new Item.Properties().stacksTo(1));
    public static final RegistrySupplier<Item> BLANK_RUNE = register("blank_rune", Item::new, new Item.Properties());
    /** Takes one infusion and turns into a {@link #SCROLL} (requirements §10.3). */
    public static final RegistrySupplier<Item> BLANK_SCROLL = register("blank_scroll", Item::new, new Item.Properties().stacksTo(16));
    /** Works from anywhere in its owner's inventory (requirements §10.3); "empty" until something is burnt into it. */
    public static final RegistrySupplier<Item> AMULET = register("amulet", Item::new, new Item.Properties().stacksTo(1));
    /** Takes one sustained circle and turns into a {@link #CIRCLE_STONE} (requirements §10.3). */
    public static final RegistrySupplier<Item> BLANK_CIRCLE_STONE = register("blank_circle_stone", Item::new, new Item.Properties().stacksTo(16));
    /** Made from a blank circle stone by infusion only, so it is not in the creative tab. */
    public static final RegistrySupplier<CircleStoneItem> CIRCLE_STONE = register("circle_stone",
            properties -> new CircleStoneItem(ThaumoryBlocks.CIRCLE_STONE.get(), properties), new Item.Properties().stacksTo(1).useBlockDescriptionPrefix());
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
    public static final RegistrySupplier<BlockItem> ARCANE_IRON_CIRCLE_CORE =
            register("arcane_iron_circle_core", properties -> new BlockItem(ThaumoryBlocks.ARCANE_IRON_CIRCLE_CORE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());
    public static final RegistrySupplier<BlockItem> AETHER_SILVER_CIRCLE_CORE =
            register("aether_silver_circle_core", properties -> new BlockItem(ThaumoryBlocks.AETHER_SILVER_CIRCLE_CORE.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());

    /** The monocle's look when worn: {@code assets/thaumory/equipment/monocle.json}. */
    public static final ResourceKey<EquipmentAsset> MONOCLE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Thaumory.id("monocle"));
    /** A loupe worn on the head (requirements §17.6): it shows what the loupe does, without scanning. */
    public static final RegistrySupplier<Item> MONOCLE = register("monocle", Item::new, new Item.Properties().stacksTo(1)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setAsset(MONOCLE_ASSET)
                    .setEquipSound(SoundEvents.ARMOR_EQUIP_GOLD).setDamageOnHurt(false).build()));

    public static final RegistrySupplier<BlockItem> ARCANE_CRYSTAL =
            register("arcane_crystal", properties -> new BlockItem(ThaumoryBlocks.ARCANE_CRYSTAL.get(), properties),
                    new Item.Properties().useBlockDescriptionPrefix());
    public static final RegistrySupplier<Item> ARCANE_CRYSTAL_SHARD = register("arcane_crystal_shard", Item::new, new Item.Properties());
    public static final RegistrySupplier<FluxCrystalItem> FLUX_CRYSTAL = register("flux_crystal", FluxCrystalItem::new,
            new Item.Properties().stacksTo(16));

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

    /**
     * Every Thaumory item, in the order the creative tab shows them. Runes come once per aspect, and
     * the empty jar is followed by a full one for each aspect.
     */
    private static final List<RegistrySupplier<? extends Item>> TAB_ORDER = List.of(
            ARCANE_CRYSTAL, ARCANE_CRYSTAL_SHARD, ARCANE_LOUPE, MONOCLE, WAND, GOLD_WAND_CAP, ARCANE_IRON_WAND_CAP, AETHER_SILVER_WAND_CAP,
            CRYSTAL_WAND_CORE, BLANK_FOCUS, LIGHT_FOCUS, ARCANE_CODEX, CRUCIBLE, JAR, LABEL, PIPE, FILTER_PIPE, VALVE, PUMP, BLANK_RUNE, RUNE, CIRCLE_CORE,
            ARCANE_IRON_CIRCLE_CORE, AETHER_SILVER_CIRCLE_CORE, PEDESTAL, BLANK_SCROLL, AMULET, BLANK_CIRCLE_STONE,
            CHALK, AMPLIFYING_CHALK, EXTENDING_CHALK, ECONOMIZING_CHALK, STABILIZING_CHALK, POLLUTED_SOIL, POLLUTED_STONE, FLUX_CRYSTAL);
    private static final List<RegistrySupplier<? extends Item>> TAB_ORDER_EQUIPMENT =
            Stream.concat(ARCANE_IRON.all().stream(), AETHER_SILVER.all().stream()).<RegistrySupplier<? extends Item>>map(item -> item).toList();

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("thaumory", () -> CreativeTabRegistry.create(builder -> builder
            .title(Component.translatable("itemGroup.thaumory"))
            .icon(() -> new ItemStack(ARCANE_CODEX.get()))
            .displayItems((parameters, output) -> {
                TAB_ORDER.forEach(item -> {
                    if (item == RUNE) {
                        ThaumoryApi.aspects().all().forEach(aspect -> output.accept(RuneItem.of(aspect)));
                    } else if (item == JAR) {
                        output.accept(item.get());
                        ThaumoryApi.aspects().all().forEach(aspect -> output.accept(fullJar(aspect)));
                    } else {
                        output.accept(item.get());
                    }
                });
                TAB_ORDER_EQUIPMENT.forEach(item -> output.accept(item.get()));
            })));

    /** A jar filled to the default capacity with one aspect, unlabeled. */
    private static ItemStack fullJar(Aspect aspect) {
        ItemStack jar = new ItemStack(JAR.get());
        jar.set(ThaumoryComponents.JAR_CONTENTS.get(),
                JarContents.EMPTY.withAspects(AspectList.builder().add(aspect, JarSettings.DEFAULT.capacity()).build()));
        return jar;
    }

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
