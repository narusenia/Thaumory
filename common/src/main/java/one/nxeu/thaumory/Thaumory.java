package one.nxeu.thaumory;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.aspect.data.ItemAspectReloadListener;
import one.nxeu.thaumory.aspect.estimate.AspectEstimation;
import one.nxeu.thaumory.aspect.estimate.VanillaRecipeAdapters;
import one.nxeu.thaumory.aspect.estimate.VanillaWorldChanges;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.command.ThaumoryCommands;
import one.nxeu.thaumory.crucible.CrucibleSettings;
import one.nxeu.thaumory.data.SettingsFileReloadListener;
import one.nxeu.thaumory.flux.FluxManager;
import one.nxeu.thaumory.flux.FluxSettings;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.knowledge.KnowledgeManager;
import one.nxeu.thaumory.network.AspectSync;
import one.nxeu.thaumory.scan.ItemScanner;
import one.nxeu.thaumory.scan.ScanSettings;

public final class Thaumory {
    public static final String MOD_ID = ThaumoryApi.MOD_ID;

    private static KnowledgeManager knowledge;

    public static void init(ThaumoryPlatform platform) {
        ThaumoryAspects.register(ThaumoryApi.aspects());
        VanillaRecipeAdapters.register(ThaumoryApi.recipeAdapters());
        VanillaWorldChanges.register(ThaumoryApi.recipeAdapters());
        FluxManager flux = new FluxManager(platform.fluxStorage());
        ThaumoryApi.provideFlux(flux);
        knowledge = new KnowledgeManager(platform.knowledgeStorage());
        ThaumoryBlocks.register();
        ThaumoryItems.register();

        ReloadListenerRegistry.register(PackType.SERVER_DATA, new ItemAspectReloadListener(), id("item_aspects"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/flux.json"), FluxSettings.CODEC, FluxSettings.DEFAULT, flux::updateSettings), id("flux"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/scanning.json"), ScanSettings.CODEC, ScanSettings.DEFAULT, ItemScanner::updateSettings), id("scanning"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/crucible.json"), CrucibleSettings.CODEC, CrucibleSettings.DEFAULT, CrucibleBlockEntity::updateSettings), id("crucible"));
        CommandRegistrationEvent.EVENT.register(
                (dispatcher, context, selection) -> ThaumoryCommands.register(dispatcher, context, flux));
        AspectEstimation.registerEvents();
        AspectSync.register();
        knowledge.registerEvents();
        ItemScanner.register();
    }

    /** Every player's knowledge. Available once {@link #init} has run. */
    public static KnowledgeManager knowledge() {
        return knowledge;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
