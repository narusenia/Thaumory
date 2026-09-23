package one.nxeu.thaumory.fabric.datagen;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.*;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;

/** Hand-written aspects for vanilla items that recipes cannot reach (requirements §2.3). */
final class VanillaItemAspects {
    private VanillaItemAspects() {}

    static ItemAspectFile build() {
        return new ItemAspectFileBuilder()
                .tag(ItemTags.LOGS, a(HERBA, 16), a(TERRA, 4))
                .item(Items.OAK_LOG, a(HERBA, 16), a(TERRA, 8))
                .tag(ItemTags.WOOL, a(BESTIA, 8), a(AER, 4))
                .item(Items.DIRT, a(TERRA, 4))
                .item(Items.STONE, a(TERRA, 8))
                .item(Items.COBBLESTONE, a(TERRA, 8), a(CHAOS, 2))
                .item(Items.RAW_IRON, a(METALLUM, 16), a(TERRA, 4))
                .none(Items.BARRIER)
                .build();
    }

    private static AspectStack a(Aspect aspect, int amount) {
        return new AspectStack(aspect, amount);
    }
}
