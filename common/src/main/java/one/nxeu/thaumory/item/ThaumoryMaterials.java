package one.nxeu.thaumory.item;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import one.nxeu.thaumory.Thaumory;

/**
 * The arcane metals' tool and armor stats (requirements §10.2). They stay close to iron and diamond on purpose:
 * what sets them apart is the infusion capacity, not the raw numbers.
 */
public final class ThaumoryMaterials {
    public static final TagKey<Item> REPAIRS_ARCANE_IRON = TagKey.create(Registries.ITEM, Thaumory.id("repairs_arcane_iron"));
    public static final TagKey<Item> REPAIRS_AETHER_SILVER = TagKey.create(Registries.ITEM, Thaumory.id("repairs_aether_silver"));

    public static final ResourceKey<EquipmentAsset> ARCANE_IRON_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Thaumory.id("arcane_iron"));
    public static final ResourceKey<EquipmentAsset> AETHER_SILVER_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Thaumory.id("aether_silver"));

    /** Mines like iron, lasts longer than iron. */
    public static final ToolMaterial ARCANE_IRON_TOOL =
            new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 350, 6.0F, 2.0F, 18, REPAIRS_ARCANE_IRON);
    /** Mines like diamond, wears out before diamond. */
    public static final ToolMaterial AETHER_SILVER_TOOL =
            new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1200, 8.0F, 3.0F, 22, REPAIRS_AETHER_SILVER);

    public static final ArmorMaterial ARCANE_IRON_ARMOR = new ArmorMaterial(21, defense(2, 5, 6, 2), 18,
            SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, REPAIRS_ARCANE_IRON, ARCANE_IRON_ASSET);
    public static final ArmorMaterial AETHER_SILVER_ARMOR = new ArmorMaterial(26, defense(3, 6, 8, 3), 22,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 2.0F, 0.0F, REPAIRS_AETHER_SILVER, AETHER_SILVER_ASSET);

    private ThaumoryMaterials() {}

    /** Defense per piece. The body slot (horses, wolves) never takes these metals, so it gets nothing. */
    private static Map<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        return new EnumMap<>(Map.of(ArmorType.BOOTS, boots, ArmorType.LEGGINGS, leggings, ArmorType.CHESTPLATE, chestplate,
                ArmorType.HELMET, helmet, ArmorType.BODY, 0));
    }
}
