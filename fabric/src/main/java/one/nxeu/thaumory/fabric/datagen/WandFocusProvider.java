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
import net.minecraft.world.item.Item;
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
        focus(output, ThaumoryItems.LIGHT_FOCUS.get(), ThaumoryFocusSpells.LIGHT, Map.of(ThaumoryAspects.LUX.id(), 1), 10, Map.of("range", 32.0));
        focus(output, ThaumoryItems.FIRE_FOCUS.get(), ThaumoryFocusSpells.FIRE, Map.of(ThaumoryAspects.IGNIS.id(), 2), 15,
                Map.of("damage", 4.0, "burn_seconds", 4.0, "speed", 1.5));
        focus(output, ThaumoryItems.FROST_FOCUS.get(), ThaumoryFocusSpells.FROST, Map.of(ThaumoryAspects.AQUA.id(), 2), 20,
                Map.of("range", 6.0, "half_angle", 30.0, "slow_seconds", 5.0));
        focus(output, ThaumoryItems.LIGHTNING_FOCUS.get(), ThaumoryFocusSpells.LIGHTNING, Map.of(ThaumoryAspects.TEMPESTAS.id(), 3), 30,
                Map.of("range", 24.0, "damage", 5.0, "chain_radius", 4.0, "chain_count", 2.0));
        focus(output, ThaumoryItems.DIGGING_FOCUS.get(), ThaumoryFocusSpells.DIGGING,
                Map.of(ThaumoryAspects.TERRA.id(), 1, ThaumoryAspects.BELLUM.id(), 1), 5, Map.of("range", 16.0));
        focus(output, ThaumoryItems.LEAP_FOCUS.get(), ThaumoryFocusSpells.LEAP,
                Map.of(ThaumoryAspects.ARCANUM.id(), 2, ThaumoryAspects.AER.id(), 2), 20, Map.of("distance", 8.0));
        focus(output, ThaumoryItems.EXCHANGE_FOCUS.get(), ThaumoryFocusSpells.EXCHANGE,
                Map.of(ThaumoryAspects.ORDO.id(), 1, ThaumoryAspects.CHAOS.id(), 1), 5, Map.of("range", 6.0));
    }

    private static void focus(BiConsumer<Identifier, WandFocus> output, Item item, Identifier spell, Map<Identifier, Integer> cost,
            int cooldown, Map<String, Double> settings) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        output.accept(id, new WandFocus(id, spell, cost, cooldown, settings));
    }

    @Override
    public String getName() {
        return "Thaumory wand foci";
    }
}
