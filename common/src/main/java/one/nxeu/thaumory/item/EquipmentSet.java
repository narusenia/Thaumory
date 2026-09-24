package one.nxeu.thaumory.item;

import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import net.minecraft.world.item.Item;

/** One arcane metal's ingot and the tools and armor made from it (requirements §10.2). */
public record EquipmentSet(
        RegistrySupplier<Item> ingot,
        RegistrySupplier<Item> sword,
        RegistrySupplier<Item> pickaxe,
        RegistrySupplier<Item> axe,
        RegistrySupplier<Item> shovel,
        RegistrySupplier<Item> hoe,
        RegistrySupplier<Item> helmet,
        RegistrySupplier<Item> chestplate,
        RegistrySupplier<Item> leggings,
        RegistrySupplier<Item> boots) {

    public List<RegistrySupplier<Item>> tools() {
        return List.of(sword, pickaxe, axe, shovel, hoe);
    }

    public List<RegistrySupplier<Item>> armor() {
        return List.of(helmet, chestplate, leggings, boots);
    }

    /** The ingot, then the tools, then the armor: the creative tab's order. */
    public List<RegistrySupplier<Item>> all() {
        return List.of(ingot, sword, pickaxe, axe, shovel, hoe, helmet, chestplate, leggings, boots);
    }
}
