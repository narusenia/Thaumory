package one.nxeu.thaumory.research;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.alchemy.AlchemyRecipe;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.knowledge.KnowledgeManager;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.network.ResearchViewPayload;
import one.nxeu.thaumory.sound.ThaumorySounds;
import one.nxeu.thaumory.text.ThaumoryText;

/**
 * Moves players through the chapters and hints whenever their knowledge changes, tells them what
 * they completed or found, and keeps their book's view of the chapters up to date. Every few
 * seconds it looks at everyone again, to catch up with reloaded datapacks.
 */
public final class ResearchProgress implements KnowledgeManager.Research {
    private static final int CHECK_INTERVAL = 100;

    private final Map<UUID, ResearchView> sent = new HashMap<>();

    public void register(KnowledgeManager knowledge) {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(ResearchViewPayload.TYPE, ResearchViewPayload.STREAM_CODEC);
        }
        knowledge.setResearch(this);
        TickEvent.SERVER_POST.register(server -> {
            if (server.getTickCount() % CHECK_INTERVAL == 0) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PlayerKnowledge current = knowledge.update(player, UnaryOperator.identity());
                    sendIfChanged(player, current);
                }
            }
        });
    }

    @Override
    public PlayerKnowledge advance(ServerPlayer player, PlayerKnowledge knowledge) {
        Research research = ResearchData.research();
        Research.Advance advance = research.advance(new KnowledgeFacts(knowledge), knowledge.chapters(), knowledge.hints());
        if (advance.isEmpty()) {
            return knowledge;
        }
        PlayerKnowledge result = knowledge;
        for (Identifier chapter : advance.chapters()) {
            result = result.withChapter(chapter);
            if (!research.chapters().get(chapter).conditions().isEmpty()) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ThaumorySounds.DISCOVERY.get(),
                        SoundSource.PLAYERS, 0.8f, 1.0f);
                player.sendSystemMessage(Component.translatable("message.thaumory.chapter.completed",
                        ThaumoryText.withEffect(Component.translatable(Chapter.titleKey(chapter)).withColor(0xE8C87A), TextEffect.STREAK)));
            }
        }
        for (Identifier hint : advance.hints()) {
            result = result.withHint(hint);
            player.sendSystemMessage(Component.translatable("message.thaumory.hint.appeared").withColor(0xCC99FF));
        }
        return result;
    }

    @Override
    public void synced(ServerPlayer player, PlayerKnowledge knowledge) {
        sendIfChanged(player, knowledge);
    }

    private void sendIfChanged(ServerPlayer player, PlayerKnowledge knowledge) {
        ResearchView view = view(player.level().getServer(), knowledge);
        if (!view.equals(sent.get(player.getUUID()))) {
            sent.put(player.getUUID(), view);
            NetworkManager.sendToPlayer(player, new ResearchViewPayload(view));
        }
    }

    /** Forgets what a player who left was sent, so they get everything when they come back. */
    public void forget(ServerPlayer player) {
        sent.remove(player.getUUID());
    }

    /** Whether {@code player} (or nobody, when it is not known who) may use the alchemy recipe. */
    public static boolean canUse(Optional<ServerPlayer> player, Identifier recipe) {
        return ResearchData.research().canUse(recipe, player.map(p -> Thaumory.knowledge().get(p).chapters()).orElse(Set.of()));
    }

    static ResearchView view(MinecraftServer server, PlayerKnowledge knowledge) {
        Research research = ResearchData.research();
        KnowledgeFacts facts = new KnowledgeFacts(knowledge);
        List<ResearchView.ChapterView> chapters = new ArrayList<>();
        for (Identifier id : research.visible(knowledge.chapters())) {
            Chapter chapter = research.chapters().get(id);
            List<ResearchView.ConditionLine> lines = chapter.conditions().stream()
                    .map(condition -> new ResearchView.ConditionLine(describe(condition, facts, knowledge), condition.progress(facts).met()))
                    .toList();
            List<Identifier> unlocks = chapter.unlocks().stream()
                    .flatMap(recipe -> server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, recipe)).stream())
                    .filter(holder -> holder.value() instanceof AlchemyRecipe)
                    .map(holder -> BuiltInRegistries.ITEM.getKey(((AlchemyRecipe) holder.value()).result().create().getItem()))
                    .toList();
            chapters.add(new ResearchView.ChapterView(id, chapter.icon(), node(research, id), knowledge.hasCompleted(id), lines, unlocks));
        }
        List<ResearchView.Node> unknown = research.unknown(knowledge.chapters()).stream().map(id -> node(research, id)).toList();
        List<ResearchView.CategoryView> categories = research.shownCategories(knowledge.chapters()).stream()
                .map(id -> new ResearchView.CategoryView(id, research.categories().get(id).icon(), research.categories().get(id).background()))
                .toList();
        return new ResearchView(categories, List.copyOf(chapters), unknown);
    }

    private static ResearchView.Node node(Research research, Identifier id) {
        ChapterLayout.Cell cell = research.cell(id);
        return new ResearchView.Node(research.categoryOf(id), cell.x(), cell.y(), research.chapters().get(id).requires());
    }

    private static Component describe(ResearchCondition condition, ResearchFacts facts, PlayerKnowledge knowledge) {
        ResearchCondition.Progress progress = condition.progress(facts);
        return switch (condition) {
            case ResearchCondition.Scanned scanned when scanned.item().isPresent() -> Component.translatable(
                    "codex.thaumory.condition.scan_item", BuiltInRegistries.ITEM.getOptional(scanned.item().get())
                            .map(item -> item.getDefaultInstance().getHoverName())
                            .orElseGet(() -> Component.literal(scanned.item().get().toString())));
            case ResearchCondition.Scanned scanned when scanned.tag().isPresent() -> Component.translatable(
                    "codex.thaumory.condition.scan_tag", "#" + scanned.tag().get());
            case ResearchCondition.Scanned scanned -> Component.translatable("codex.thaumory.condition.scan_count",
                    progress.needed(), progress.current());
            case ResearchCondition.Aspects aspects when aspects.count().isPresent() -> Component.translatable(
                    "codex.thaumory.condition.aspects_count", progress.needed(), progress.current());
            case ResearchCondition.Aspects aspects -> {
                MutableComponent names = Component.empty();
                for (int i = 0; i < aspects.aspects().size(); i++) {
                    Identifier id = aspects.aspects().get(i);
                    if (i > 0) {
                        names.append(", ");
                    }
                    names.append(ThaumoryApi.aspects().get(id).map(aspect -> (Component) AspectText.name(aspect, knowledge.knowsAspect(id)))
                            .orElseGet(() -> Component.literal(id.toString())));
                }
                yield Component.translatable("codex.thaumory.condition.aspects_list", names);
            }
            case ResearchCondition.Circles circles when circles.effect().isPresent() -> Component.translatable(
                    circles.success() ? "codex.thaumory.condition.circle_effect_success" : "codex.thaumory.condition.circle_effect_failure",
                    Component.translatable(circles.effect().get().toLanguageKey("circle_effect")), progress.needed(), progress.current());
            case ResearchCondition.Circles circles -> Component.translatable(
                    circles.success() ? "codex.thaumory.condition.circle_success" : "codex.thaumory.condition.circle_failure",
                    progress.needed(), progress.current());
        };
    }
}
