package one.nxeu.thaumory.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.SplitPacketTransformer;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.stone.BurntCircle;
import one.nxeu.thaumory.client.codex.ArcaneCodexScreen;
import one.nxeu.thaumory.client.entity.VoidRemnantRenderer;
import one.nxeu.thaumory.client.particle.AspectMoteParticle;
import one.nxeu.thaumory.entity.ThaumoryEntities;
import one.nxeu.thaumory.infusion.Infusion;
import one.nxeu.thaumory.infusion.InfusionText;
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.item.ArcaneCodexItem;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.item.TranscriptItem;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.knowledge.Transcript.AspectTranscript;
import one.nxeu.thaumory.knowledge.Transcript.CircleTranscript;
import one.nxeu.thaumory.network.AspectSyncPayload;
import one.nxeu.thaumory.network.CapacitySyncPayload;
import one.nxeu.thaumory.network.FluxReadingPayload;
import one.nxeu.thaumory.network.KnowledgeSyncPayload;
import one.nxeu.thaumory.network.PipeReadingPayload;
import one.nxeu.thaumory.network.ResearchViewPayload;
import one.nxeu.thaumory.network.UseInfusionPayload;
import one.nxeu.thaumory.network.WandPartSyncPayload;
import one.nxeu.thaumory.particle.ThaumoryParticles;
import one.nxeu.thaumory.pouch.EssentiaPouchItem;
import one.nxeu.thaumory.pouch.ThaumoryMenus;
import one.nxeu.thaumory.wand.WandBuild;
import one.nxeu.thaumory.wand.WandDisplay;
import one.nxeu.thaumory.wand.WandFocus;
import one.nxeu.thaumory.wand.WandPart;
import one.nxeu.thaumory.wand.WandParts;
import org.slf4j.Logger;

public final class ThaumoryClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Uses the active effect of the item in hand, or of the armor worn (requirements §10.2). */
    private static final KeyMapping USE_INFUSION = new KeyMapping("key.thaumory.use_infusion", InputConstants.KEY_V,
            KeyMapping.Category.register(Thaumory.id("thaumory")));
    /** Held to choose the focus of the wand in hand (requirements §17.7). */
    private static final KeyMapping SELECT_FOCUS = new KeyMapping("key.thaumory.select_focus", InputConstants.KEY_G,
            USE_INFUSION.getCategory());

    private ThaumoryClient() {}

    public static void init(ParticleProviders particles, MenuScreenFactories screens) {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, AspectSyncPayload.TYPE, AspectSyncPayload.STREAM_CODEC,
                List.of(new SplitPacketTransformer()), (payload, context) -> context.queue(() -> {
                    ClientItemAspects.replace(payload.items());
                    LOGGER.info("Received aspects for {} items", payload.items().size());
                }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CapacitySyncPayload.TYPE, CapacitySyncPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> ClientCapacities.replace(payload.items(), payload.itemEssentia())));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, WandPartSyncPayload.TYPE, WandPartSyncPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> ClientWandParts.replace(payload.parts(), payload.foci())));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    ClientKnowledge.replace(payload.knowledge());
                    LOGGER.info("Received knowledge: {} scanned items, {} aspects",
                            payload.knowledge().scanned(PlayerKnowledge.ITEMS).size(), payload.knowledge().aspects().size());
                }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FluxReadingPayload.TYPE, FluxReadingPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> ClientFlux.replace(payload)));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PipeReadingPayload.TYPE, PipeReadingPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> ClientPipeReading.replace(payload)));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ResearchViewPayload.TYPE, ResearchViewPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> ClientResearch.replace(payload.view())));
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            ClientItemAspects.clear();
            ClientCapacities.clear();
            ClientKnowledge.clear();
            ClientFlux.clear();
            ClientPipeReading.clear();
            ClientResearch.clear();
        });
        KeyMappingRegistry.register(USE_INFUSION);
        KeyMappingRegistry.register(SELECT_FOCUS);
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new RingGlows(), Thaumory.id("ring_glows"));
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (USE_INFUSION.consumeClick()) {
                if (minecraft.player != null) {
                    NetworkManager.sendToServer(UseInfusionPayload.INSTANCE);
                }
            }
            while (SELECT_FOCUS.consumeClick()) {
                if (minecraft.player != null && minecraft.gui.screen() == null && minecraft.player.getMainHandItem().is(ThaumoryItems.WAND.get())) {
                    FocusMenuScreen.open(SELECT_FOCUS, minecraft.player).ifPresentOrElse(minecraft.gui::setScreen,
                            () -> minecraft.player.sendOverlayMessage(Component.translatable("message.thaumory.focus.none_in_inventory")));
                }
            }
        });
        ClientGuiEvent.RENDER_HUD.register(LoupeHud::render);
        ClientGuiEvent.RENDER_HUD.register(WandHud::render);
        BlockEntityRendererRegistry.register(ThaumoryBlocks.JAR_ENTITY.get(), JarRenderer::new);
        BlockEntityRendererRegistry.register(ThaumoryBlocks.CIRCLE_CORE_ENTITY.get(), CircleCoreRenderer::new);
        BlockEntityRendererRegistry.register(ThaumoryBlocks.CIRCLE_STONE_ENTITY.get(), CircleStoneRenderer::new);
        BlockEntityRendererRegistry.register(ThaumoryBlocks.PIPE_ENTITY.get(), PipeRenderer::new);
        EntityRendererRegistry.register(ThaumoryEntities.VOID_REMNANT, VoidRemnantRenderer::new);
        EntityRendererRegistry.register(ThaumoryEntities.FOCUS_FIREBALL, ThrownItemRenderer::new);
        screens.register(ThaumoryMenus.ESSENTIA_POUCH.get(), PouchScreen::new);
        particles.register(ThaumoryParticles.ASPECT_MOTE.get(), AspectMoteParticle.Provider::new);
        RuneTint.register();
        FilterPipeTint.register();
        ArcaneCodexItem.setScreenOpener(() -> {
            // The server answers with a fresh reading for the book's Flux warning.
            ClientFlux.clear();
            Minecraft.getInstance().gui.setScreen(new ArcaneCodexScreen());
        });
        ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> {
            appendJarContents(stack, lines);
            appendPouchJars(stack, lines);
            appendRuneAspect(stack, lines);
            appendTranscript(stack, lines);
            appendInfusions(stack, lines);
            appendBurntCircle(stack, lines);
            appendWandBuild(stack, lines);
            appendAspects(stack.getItem(), lines);
        });
    }

    private static void appendJarContents(ItemStack stack, List<Component> lines) {
        JarContents contents = stack.get(ThaumoryComponents.JAR_CONTENTS.get());
        if (contents != null) {
            lines.addAll(JarText.describe(contents, OptionalInt.empty(), ClientKnowledge.get()));
        }
    }

    /** What each jar in an Essentia pouch holds. */
    private static void appendPouchJars(ItemStack stack, List<Component> lines) {
        if (!stack.is(ThaumoryItems.ESSENTIA_POUCH.get())) {
            return;
        }
        for (ItemStack jar : EssentiaPouchItem.jars(stack)) {
            if (jar.isEmpty()) {
                continue;
            }
            AspectList aspects = jar.getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY).aspects();
            MutableComponent line = Component.literal("- ").withColor(0xAAAAAA);
            if (aspects.isEmpty()) {
                line.append(Component.translatable("tooltip.thaumory.pouch.empty_jar"));
            }
            boolean first = true;
            for (AspectStack each : aspects.sortedByAmount()) {
                line.append(first ? Component.empty() : Component.literal(" · "))
                        .append(AspectText.stack(each, ClientKnowledge.get().knowsAspect(each.aspect().id())));
                first = false;
            }
            lines.add(line);
        }
    }

    /** The runes of the circle burnt into a circle stone, slot 1 first; its name already gives the effect. */
    private static void appendBurntCircle(ItemStack stack, List<Component> lines) {
        BurntCircle burnt = stack.get(ThaumoryComponents.BURNT_CIRCLE.get());
        if (burnt == null) {
            return;
        }
        PlayerKnowledge knowledge = ClientKnowledge.get();
        CircleCombination combination = burnt.combination();
        MutableComponent line = Component.translatable("tooltip.thaumory.circle_stone.runes").withColor(0xAAAAAA);
        for (Identifier id : Stream.concat(Stream.of(combination.first(), combination.second()),
                Stream.concat(combination.parameter().stream(), combination.slot4().stream())).toList()) {
            line.append(" ").append(ThaumoryApi.aspects().get(id)
                    .map(aspect -> (Component) AspectText.name(aspect, knowledge.knowsAspect(id)))
                    .orElseGet(() -> Component.literal(id.toString())));
        }
        lines.add(line);
    }

    /** A wand's caps and core, with what each gives as the server last sent it. */
    private static void appendWandBuild(ItemStack stack, List<Component> lines) {
        if (!stack.is(ThaumoryItems.WAND.get())) {
            return;
        }
        WandBuild build = stack.getOrDefault(ThaumoryComponents.WAND_BUILD.get(), WandBuild.DEFAULT);
        Map<Identifier, WandPart> parts = ClientWandParts.get();
        MutableComponent cap = Component.translatable("tooltip.thaumory.wand.cap", partName(build.cap()));
        WandParts.cap(parts, build.cap()).ifPresent(c -> cap.append(Component.translatable("tooltip.thaumory.wand.cap_stats", c.essentia())));
        lines.add(cap.withColor(0xAAAAAA));
        MutableComponent core = Component.translatable("tooltip.thaumory.wand.core", partName(build.core()));
        WandParts.core(parts, build.core()).ifPresent(c -> core.append(Component.translatable("tooltip.thaumory.wand.core_stats",
                String.format(Locale.ROOT, "%.2f", c.power()),
                Component.translatableWithFallback("tooltip.thaumory.wand.tier." + c.incorrectFor().toLanguageKey(), c.incorrectFor().toString()))));
        lines.add(core.withColor(0xAAAAAA));
        appendWandFocus(stack, WandParts.cap(parts, build.cap()).map(WandPart.Cap::essentia).orElse(0), lines);
    }

    /** The wand's focus, then its Essentia against what the caps hold, in {@link WandDisplay}'s order. */
    private static void appendWandFocus(ItemStack stack, int capacity, List<Component> lines) {
        Identifier focusItem = stack.get(ThaumoryComponents.WAND_FOCUS.get());
        if (focusItem != null) {
            lines.add(Component.translatable("tooltip.thaumory.wand.focus", partName(focusItem)).withColor(0xAAAAAA));
        }
        AspectList stored = stack.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
        Map<Identifier, Integer> cost = focusItem == null ? Map.of()
                : Optional.ofNullable(ClientWandParts.foci().get(focusItem)).map(WandFocus::cost).orElse(Map.of());
        Map<Identifier, Integer> amounts = new HashMap<>();
        stored.stacks().forEach(s -> amounts.put(s.aspect().id(), s.amount()));
        List<Identifier> shown = WandDisplay.bars(cost, amounts).stream().map(WandDisplay.Bar::aspect).toList();
        if (shown.isEmpty()) {
            return;
        }
        PlayerKnowledge knowledge = ClientKnowledge.get();
        lines.add(Component.translatable("tooltip.thaumory.stored_essentia").withColor(0xAAAAAA));
        for (Identifier id : shown) {
            ThaumoryApi.aspects().get(id).ifPresent(aspect -> lines.add(Component.literal("  ")
                    .append(AspectText.name(aspect, knowledge.knowsAspect(id)))
                    .append(Component.literal(" " + stored.amount(aspect) + "/" + capacity).withColor(0xAAAAAA))));
        }
    }

    private static Component partName(Identifier item) {
        return BuiltInRegistries.ITEM.getOptional(item).map(i -> (Component) new ItemStack(i).getHoverName()).orElseGet(() -> Component.literal(item.toString()));
    }

    private static void appendRuneAspect(ItemStack stack, List<Component> lines) {
        RuneItem.aspect(stack).ifPresent(aspect -> lines.add(Component.translatable("tooltip.thaumory.rune.aspect",
                AspectText.name(aspect, ClientKnowledge.get().knowsAspect(aspect.id()))).withColor(0xAAAAAA)));
    }

    /** How much of its capacity the item has used, then the circle effects burnt into it, each with its level. */
    private static void appendInfusions(ItemStack stack, List<Component> lines) {
        Infusions infusions = stack.getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY);
        int capacity = ClientCapacities.of(stack.getItem());
        if (capacity > 0) {
            lines.add(Component.translatable("tooltip.thaumory.capacity", infusions.used(), capacity).withColor(0xAAAAAA));
        }
        if (infusions.list().isEmpty()) {
            return;
        }
        lines.add(Component.translatable("tooltip.thaumory.infusions").withColor(0xAAAAAA));
        for (Infusion infusion : infusions.list()) {
            MutableComponent line = Component.literal("  ").append(InfusionText.describe(infusion));
            if (infusion.active()) {
                line.append(Component.translatable("tooltip.thaumory.infusion.active").withColor(0xAAAAAA));
            }
            lines.add(line);
        }
        appendStoredEssentia(stack, infusions, lines);
    }

    /** The Essentia kept for the item's active effect, each aspect against what it holds. */
    private static void appendStoredEssentia(ItemStack stack, Infusions infusions, List<Component> lines) {
        if (infusions.storedAspects().isEmpty()) {
            return;
        }
        AspectList stored = stack.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
        PlayerKnowledge knowledge = ClientKnowledge.get();
        MutableComponent line = Component.translatable("tooltip.thaumory.stored_essentia").withColor(0xAAAAAA);
        boolean first = true;
        for (Identifier id : infusions.storedAspects().stream().sorted().toList()) {
            Optional<Aspect> aspect = ThaumoryApi.aspects().get(id);
            if (aspect.isEmpty()) {
                continue;
            }
            line.append(Component.literal(first ? " " : " · "))
                    .append(AspectText.name(aspect.get(), knowledge.knowsAspect(id)))
                    .append(Component.literal(" " + stored.amount(aspect.get()) + "/" + ClientCapacities.itemEssentia()).withColor(0xAAAAAA));
            first = false;
        }
        lines.add(line);
    }

    /** What a transcript holds, as far as the one holding it knows; a circle's effect stays unnamed. */
    private static void appendTranscript(ItemStack stack, List<Component> lines) {
        TranscriptItem.transcript(stack).ifPresent(transcript -> lines.add(switch (transcript) {
            case AspectTranscript(var aspect) -> CircleText.aspectName(aspect, ClientKnowledge.get());
            case CircleTranscript(var combination) -> CircleText.combination(combination, ClientKnowledge.get(), 0xFFAAAAAA);
        }));
    }

    /** Aspects of scanned items only; aspects the player has not worked out show as "?". */
    private static void appendAspects(Item item, List<Component> lines) {
        PlayerKnowledge knowledge = ClientKnowledge.get();
        if (!knowledge.hasScanned(PlayerKnowledge.ITEMS, BuiltInRegistries.ITEM.getKey(item))) {
            return;
        }
        AspectList aspects = ClientItemAspects.get(item);
        if (aspects.isEmpty()) {
            return;
        }
        lines.add(Component.translatable("tooltip.thaumory.aspects").withColor(0xAAAAAA));
        for (AspectStack stack : aspects.sortedByAmount()) {
            lines.add(Component.literal("  ").append(AspectText.stack(stack, knowledge.knowsAspect(stack.aspect().id()))));
        }
    }
}
