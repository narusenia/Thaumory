package one.nxeu.thaumory.alchemy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import one.nxeu.thaumory.api.aspect.AspectList;

/** A catalyst dropped into a Crucible holding {@code contents}. */
public record AlchemyInput(ItemStack catalyst, AspectList contents) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index != 0) {
            throw new IllegalArgumentException("An alchemy input has only the catalyst, not slot " + index);
        }
        return catalyst;
    }

    @Override
    public int size() {
        return 1;
    }
}
