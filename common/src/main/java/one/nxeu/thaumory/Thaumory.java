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
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.circle.effect.ThaumoryInfusionEffects;
import one.nxeu.thaumory.command.ThaumoryCommands;
import one.nxeu.thaumory.crucible.CrucibleSettings;
import one.nxeu.thaumory.data.SettingsFileReloadListener;
import one.nxeu.thaumory.entity.ThaumoryEntities;
import one.nxeu.thaumory.essentia.EssentiaLookup;
import one.nxeu.thaumory.flux.FluxManager;
import one.nxeu.thaumory.flux.FluxReadings;
import one.nxeu.thaumory.flux.FluxSettings;
import one.nxeu.thaumory.flux.FluxWorldEffects;
import one.nxeu.thaumory.flux.pollution.PollutionRules;
import one.nxeu.thaumory.infusion.InfusionCapacities;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.item.TranscriptItem;
import one.nxeu.thaumory.jar.JarSettings;
import one.nxeu.thaumory.knowledge.KnowledgeManager;
import one.nxeu.thaumory.network.AspectSync;
import one.nxeu.thaumory.network.CapacitySync;
import one.nxeu.thaumory.network.WandPartSync;
import one.nxeu.thaumory.pipe.PipeNetworks;
import one.nxeu.thaumory.pipe.PipeReadings;
import one.nxeu.thaumory.pipe.PipeSettings;
import one.nxeu.thaumory.research.ResearchData;
import one.nxeu.thaumory.research.ResearchProgress;
import one.nxeu.thaumory.rune.RuneSettings;
import one.nxeu.thaumory.scan.ItemScanner;
import one.nxeu.thaumory.scan.ScanSettings;
import one.nxeu.thaumory.particle.ThaumoryParticles;
import one.nxeu.thaumory.sound.ThaumorySounds;
import one.nxeu.thaumory.wand.FocusSelection;
import one.nxeu.thaumory.wand.WandFoci;
import one.nxeu.thaumory.wand.WandParts;
import one.nxeu.thaumory.wand.spell.ThaumoryFocusSpells;
import one.nxeu.thaumory.world.ThaumoryFeatures;

public final class Thaumory {
    public static final String MOD_ID = ThaumoryApi.MOD_ID;

    private static KnowledgeManager knowledge;
    private static FluxManager flux;
    private static EssentiaLookup essentia;

    public static void init(ThaumoryPlatform platform) {
        ThaumoryAspects.register(ThaumoryApi.aspects());
        VanillaRecipeAdapters.register(ThaumoryApi.recipeAdapters());
        VanillaWorldChanges.register(ThaumoryApi.recipeAdapters());
        ThaumoryRecipes.registerAdapters(ThaumoryApi.recipeAdapters());
        ThaumoryCircleEffects.register(ThaumoryApi.circleEffects());
        ThaumoryInfusionEffects.register(ThaumoryApi.infusionEffects());
        ThaumoryFocusSpells.register(ThaumoryApi.focusSpells());
        flux = new FluxManager(platform.fluxStorage());
        essentia = platform.essentiaLookup();
        ThaumoryApi.provideFlux(flux);
        knowledge = new KnowledgeManager(platform.knowledgeStorage());
        ThaumoryComponents.register();
        ThaumoryBlocks.register();
        ThaumoryItems.register();
        ThaumoryEntities.register();
        ThaumoryFeatures.register();
        ThaumoryParticles.register();
        ThaumorySounds.register();
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
                id("thaumory/circle.json"), CircleSettings.CODEC, CircleSettings.DEFAULT, CircleCoreBlockEntity::updateSettings), id("circle"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new SettingsFileReloadListener<>(
                id("thaumory/pipe.json"), PipeSettings.CODEC, PipeSettings.DEFAULT, PipeNetworks::updateSettings), id("pipe"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new CircleDefinitionReloadListener(), id("circle_definitions"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, InfusionCapacities.reloadListener(), id("infusion_capacity"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, WandParts.reloadListener(), id("wand_parts"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, WandFoci.reloadListener(), id("wand_foci"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new PollutionRules(), id("pollution"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ResearchData.chapterListener(), id("research_chapters"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ResearchData.hintListener(), id("research_hints"));
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ResearchData.categoryListener(), id("research_categories"));
        CommandRegistrationEvent.EVENT.register(
                (dispatcher, context, selection) -> ThaumoryCommands.register(dispatcher, context, flux));
        AspectEstimation.registerEvents();
        AspectSync.register();
        CapacitySync.register();
        WandPartSync.register();
        InfusionRuntime.register();
        FocusSelection.register();
        knowledge.registerEvents();
        ResearchProgress research = new ResearchProgress();
        research.register(knowledge);
        PlayerEvent.PLAYER_QUIT.register(research::forget);
        ItemScanner.register();
        TranscriptItem.register();
        PipeNetworks.register();
        PipeReadings.register();
        FluxReadings.register();
        new FluxWorldEffects(flux).register();
    }

    /** Flux in every chunk, with its settings. Available once {@link #init} has run. */
    public static FluxManager flux() {
        return flux;
    }

    /** Essentia storages in the world, other mods' included. Available once {@link #init} has run. */
    public static EssentiaLookup essentia() {
        return essentia;
    }

    /** Every player's knowledge. Available once {@link #init} has run. */
    public static KnowledgeManager knowledge() {
        return knowledge;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
