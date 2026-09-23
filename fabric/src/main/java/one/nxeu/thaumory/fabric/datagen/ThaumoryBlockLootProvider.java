package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.item.ThaumoryComponents;

final class ThaumoryBlockLootProvider extends FabricBlockLootSubProvider {
    ThaumoryBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generate() {
        dropSelf(ThaumoryBlocks.CRUCIBLE.get());
        dropSelf(ThaumoryBlocks.CORE.get());
        dropSelf(ThaumoryBlocks.POLLUTED_SOIL.get());
        dropSelf(ThaumoryBlocks.POLLUTED_STONE.get());
        // A broken jar keeps its Essentia and label.
        add(ThaumoryBlocks.JAR.get(), LootTable.lootTable().withPool(applyExplosionCondition(ThaumoryBlocks.JAR.get(), LootPool.lootPool()
                .add(LootItem.lootTableItem(ThaumoryBlocks.JAR.get())
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(ThaumoryComponents.JAR_CONTENTS.get()))))));
    }
}
