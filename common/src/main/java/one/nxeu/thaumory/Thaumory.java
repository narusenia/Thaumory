package one.nxeu.thaumory;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import one.nxeu.thaumory.alchemy.ThaumoryRecipes;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.aspect.data.ItemAspectReloadListener;
import one.nxeu.thaumory.aspect.estimate.AspectEstimation;
import one.nxeu.thaumory.aspect.estimate.VanillaRecipeAdapters;
import one.nxeu.thaumory.aspect.estimate.VanillaWorldChanges;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.command.ThaumoryCommands;
import one.nxeu.thaumory.crucible.CrucibleSettings;
import one.nxeu.thaumory.data.SettingsFileReloadListener;
import one.nxeu.thaumory.entity.ThaumoryEntities;
import one.nxeu.thaumory.flux.FluxManager;
import one.nxeu.thaumory.flux.FluxReadings;
import one.nxeu.thaumory.flux.FluxSettings;
import one.nxeu.thaumory.flux.FluxWorldEffects;
import one.nxeu.thaumory.flux.pollution.PollutionRules;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.jar.JarSettings;
import one.nxeu.thaumory.knowledge.KnowledgeManager;
import one.nxeu.thaumory.research.ResearchData;
import one.nxeu.thaumory.research.ResearchProgress;
import one.nxeu.thaumory.network.AspectSync;
import one.nxeu.thaumory.rune.RuneSettings;
import one.nxeu.thaumory.scan.ItemScanner;
import one.nxeu.thaumory.scan.ScanSettings;

public final class Thaumory {
    public static final String MOD_ID = ThaumoryApi.MOD_ID;

    private static KnowledgeManager knowledge;
    private static FluxManager flux;

    public static void init(ThaumoryPlatform platform) {
        ThaumoryAspects.register(ThaumoryApi.aspects());
        VanillaRecipeAdapters.register(ThaumoryApi.recipeAdapters());
        VanillaWorldChanges.register(ThaumoryApi.recipeAdapters());
        ThaumoryRecipes.registerAdapters(ThaumoryApi.recipeAdapters());
        ThaumoryCircleEffects.register(ThaumoryApi.circleEffects());
        flux = new FluxManager(platform.fluxStorage());
        ThaumoryApi.provideFlux(flux);
        knowledge = new KnowledgeManager(platform.knowledgeStorage());
        ThaumoryComponents.register();
        ThaumoryBlocks.register();
        ThaumoryItems.register();
        ThaumoryEntities.register();
        ThaumoryRecipes.register();

        ReloadListenerRegistry.register(PackType.SERVER_DATA, new ItemAspectReloadListener(), id("item_aspects"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/flux.json"), FluxSettings.CODEC, FluxSettings.DEFAULT, flux::updateSettings), id("flux"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/scanning.json"), ScanSettings.CODEC, ScanSettings.DEFAULT, ItemScanner::updateSettings), id("scanning"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/crucible.json"), CrucibleSettings.CODEC, CrucibleSettings.DEFAULT, CrucibleBlockEntity::updateSettings), id("crucible"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/jar.json"), JarSettings.CODEC, JarSettings.DEFAULT, JarBlockEntity::updateSettings), id("jar"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/rune.json"), RuneSettings.CODEC, RuneSettings.DEFAULT, RuneItem::updateSettings), id("rune"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/circle.json"), CircleSettings.CODEC, CircleSettings.DEFAULT, CoreBlockEntity::updateSettings), id("circle"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new CircleDefinitionReloadListener(), id("circle_definitions"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new PollutionRules(), id("pollution"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ResearchData.chapterListener(), id("research_chapters"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ResearchData.hintListener(), id("research_hints"));
        CommandRegistrationEvent.EVENT.register(
                (dispatcher, context, selection) -> ThaumoryCommands.register(dispatcher, context, flux));
        AspectEstimation.registerEvents();
        AspectSync.register();
        knowledge.registerEvents();
        ResearchProgress research = new ResearchProgress();
        research.register(knowledge);
        PlayerEvent.PLAYER_QUIT.register(research::forget);
        ItemScanner.register();
        FluxReadings.register();
        new FluxWorldEffects(flux).register();
    }

    /** Flux in every chunk, with its settings. Available once {@link #init} has run. */
    public static FluxManager flux() {
        return flux;
    }

    /** Every player's knowledge. Available once {@link #init} has run. */
    public static KnowledgeManager knowledge() {
        return knowledge;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
