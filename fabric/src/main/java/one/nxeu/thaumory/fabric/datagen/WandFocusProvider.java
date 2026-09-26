package one.nxeu.thaumory.fabric.datagen;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.wand.WandFocus;
import one.nxeu.thaumory.wand.spell.ThaumoryFocusSpells;

/** Writes {@code data/thaumory/thaumory/wand_focus/*.json} (requirements §17.7): each focus, its cost and its rest. */
final class WandFocusProvider extends FabricCodecDataProvider<WandFocus> {
    WandFocusProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/wand_focus", WandFocus.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, WandFocus> output, HolderLookup.Provider registries) {
        Identifier light = BuiltInRegistries.ITEM.getKey(ThaumoryItems.LIGHT_FOCUS.get());
        output.accept(light, new WandFocus(light, ThaumoryFocusSpells.LIGHT, Map.of(ThaumoryAspects.LUX.id(), 1), 10, Map.of("range", 32.0)));
    }

    @Override
    public String getName() {
        return "Thaumory wand foci";
    }
}
