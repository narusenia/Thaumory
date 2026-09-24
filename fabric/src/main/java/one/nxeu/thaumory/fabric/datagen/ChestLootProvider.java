package one.nxeu.thaumory.fabric.datagen;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.SimpleFabricLootTableSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.Transcript;

/** Writes {@code data/thaumory/loot_table/chests/*.json}: the chest of an old circle (requirements §17.3). */
final class ChestLootProvider extends SimpleFabricLootTableSubProvider {
    static final ResourceKey<LootTable> OLD_CIRCLE = ResourceKey.create(Registries.LOOT_TABLE, Thaumory.id("chests/old_circle"));

    ChestLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, LootContextParamSets.CHEST);
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        LootPool.Builder transcripts = LootPool.lootPool().setRolls(ContextIntProviders.between(1, 2));
        for (Aspect primal : List.of(ThaumoryAspects.IGNIS, ThaumoryAspects.AER, ThaumoryAspects.VITA, ThaumoryAspects.AQUA,
                ThaumoryAspects.TERRA, ThaumoryAspects.MORS)) {
            transcripts.add(transcript(new Transcript.AspectTranscript(primal.id())).setWeight(3));
        }
        for (Aspect compound : List.of(ThaumoryAspects.LUX, ThaumoryAspects.ORDO, ThaumoryAspects.HERBA, ThaumoryAspects.VINCULUM)) {
            transcripts.add(transcript(new Transcript.AspectTranscript(compound.id())).setWeight(2));
        }
        // The three circles the ruins hold, now and then written out.
        for (List<Aspect> runes : List.of(List.of(ThaumoryAspects.LUX, ThaumoryAspects.IGNIS), List.of(ThaumoryAspects.VITA, ThaumoryAspects.ORDO),
                List.of(ThaumoryAspects.ORDO, ThaumoryAspects.LUX))) {
            transcripts.add(transcript(new Transcript.CircleTranscript(
                    new CircleCombination(runes.get(0).id(), runes.get(1).id(), Optional.empty()))).setWeight(1));
        }

        output.accept(OLD_CIRCLE, LootTable.lootTable()
                .withPool(transcripts)
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(2))
                        .add(LootItem.lootTableItem(ThaumoryItems.BLANK_RUNE.get()).setWeight(3)
                                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))
                        .add(LootItem.lootTableItem(ThaumoryItems.CHALK.get()).setWeight(2)
                                .apply(SetItemDamageFunction.setDamage(ContextFloatProviders.between(0.3f, 0.7f))))
                        .add(EmptyLootItem.emptyItem().setWeight(1)))
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                        .add(LootItem.lootTableItem(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get())
                                .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 4)))))
                .withPool(LootPool.lootPool().setRolls(ContextIntProviders.between(2, 4))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(3).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.INK_SAC).setWeight(2).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(3).apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(1).apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))));
    }

    private static UniformContainerBase.Builder<?> transcript(Transcript transcript) {
        return LootItem.lootTableItem(ThaumoryItems.TRANSCRIPT.get()).apply(SetComponentsFunction.setComponent(ThaumoryComponents.TRANSCRIPT.get(), transcript));
    }

    /** Unused: Fabric's datagen calls {@link #generate} directly. */
    @Override
    public void run() {}

    @Override
    public String getName() {
        return "Thaumory chest loot";
    }
}
