package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;

final class ThaumoryBlockLootProvider extends FabricBlockLootSubProvider {
    ThaumoryBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generate() {
        dropSelf(ThaumoryBlocks.CRUCIBLE.get());
        dropSelf(ThaumoryBlocks.CIRCLE_CORE.get());
        dropSelf(ThaumoryBlocks.PIPE.get());
        dropSelf(ThaumoryBlocks.FILTER_PIPE.get());
        dropSelf(ThaumoryBlocks.VALVE.get());
        dropSelf(ThaumoryBlocks.PUMP.get());
        dropSelf(ThaumoryBlocks.POLLUTED_SOIL.get());
        dropSelf(ThaumoryBlocks.POLLUTED_STONE.get());
        // Like an amethyst cluster: the crystal itself with Silk Touch, 2 to 4 shards (more with Fortune)
        // with a pickaxe, otherwise 1.
        add(ThaumoryBlocks.ARCANE_CRYSTAL.get(), block -> createSilkTouchDispatchTable(block,
                LootItem.lootTableItem(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get())
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(2, 4)))
                        .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))
                        .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(items, ItemTags.CLUSTER_MAX_HARVESTABLES)))
                        .otherwise((LootPoolEntryContainer.Builder<?>) applyExplosionDecay(block, LootItem.lootTableItem(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get())))));
        // A broken jar keeps its Essentia and label.
        add(ThaumoryBlocks.JAR.get(), LootTable.lootTable().withPool(applyExplosionCondition(ThaumoryBlocks.JAR.get(), LootPool.lootPool()
                .add(LootItem.lootTableItem(ThaumoryBlocks.JAR.get())
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(ThaumoryComponents.JAR_CONTENTS.get()))))));
    }
}
