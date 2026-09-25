package one.nxeu.thaumory.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import one.nxeu.thaumory.block.stone.BurntCircle;

/** A circle stone with a circle burnt into it, named after the circle's effect: "Healing Circle Stone". */
public final class CircleStoneItem extends BlockItem {
    public CircleStoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        BurntCircle burnt = stack.get(ThaumoryComponents.BURNT_CIRCLE.get());
        return burnt == null ? super.getName(stack)
                : Component.translatable(getDescriptionId() + ".named", Component.translatable(burnt.effect().toLanguageKey("circle_effect")));
    }
}
