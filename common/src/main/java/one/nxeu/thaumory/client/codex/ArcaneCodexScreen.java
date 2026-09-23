package one.nxeu.thaumory.client.codex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.flux.FluxStage;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.client.ClientFlux;
import one.nxeu.thaumory.client.ClientKnowledge;
import one.nxeu.thaumory.client.ClientResearch;
import one.nxeu.thaumory.client.FluxStageText;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge.CircleOutcome;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.research.Chapter;
import one.nxeu.thaumory.research.Hint;
import one.nxeu.thaumory.research.ResearchView;

/** The Arcane Codex: tabs on the left, a scrolling list on the right. Reads the client's copy of the player's knowledge. */
public final class ArcaneCodexScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int HEIGHT = 200;
    private static final int TAB_WIDTH = 76;
    private static final int PADDING = 8;
    private static final int LINE = 11;
    private static final int ICON = 18;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int WHITE = 0xFFFFFFFF;

    /** Remembered while the game runs, so the book reopens where it was left. */
    private static Tab lastTab = Tab.ASPECTS;

    private final Map<Tab, Button> tabButtons = new EnumMap<>(Tab.class);
    private Tab tab = lastTab;
    private double scroll;
    private int left;
    private int top;

    private enum Tab {
        CHAPTERS, ASPECTS, SCANNED, CIRCLES, HINTS;

        Component title() {
            return Component.translatable("codex.thaumory.tab." + name().toLowerCase(Locale.ROOT));
        }
    }

    public ArcaneCodexScreen() {
        super(Component.translatable("item.thaumory.arcane_codex"));
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        tabButtons.clear();
        int y = top + PADDING;
        for (Tab each : Tab.values()) {
            Button button = Button.builder(each.title(), b -> select(each))
                    .bounds(left + PADDING, y, TAB_WIDTH - PADDING, 20).build();
            tabButtons.put(each, addRenderableWidget(button));
            y += 24;
        }
        select(tab);
    }

    private void select(Tab selected) {
        tab = lastTab = selected;
        scroll = 0;
        tabButtons.forEach((each, button) -> button.active = each != selected);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int contentLeft() {
        return left + TAB_WIDTH + PADDING;
    }

    private int contentTop() {
        return top + PADDING;
    }

    private int contentWidth() {
        return WIDTH - TAB_WIDTH - PADDING * 2;
    }

    private int contentHeight() {
        return HEIGHT - PADDING * 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xE0201A24);
        graphics.fill(left + TAB_WIDTH, top + PADDING, left + TAB_WIDTH + 1, top + HEIGHT - PADDING, 0xFF5A4B63);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        PlayerKnowledge knowledge = ClientKnowledge.get();
        graphics.enableScissor(contentLeft(), contentTop(), contentLeft() + contentWidth(), contentTop() + contentHeight());
        int contentSize = tab == Tab.SCANNED
                ? drawScanned(graphics, knowledge, mouseX, mouseY)
                : drawLines(graphics, lines(knowledge));
        graphics.disableScissor();
        scroll = Math.clamp(scroll, 0, Math.max(0, contentSize - contentHeight()));
        drawFluxWarning(graphics);
    }

    /** A band above the book when the player stands where Flux has reached stagnation (requirements §5). */
    private void drawFluxWarning(GuiGraphicsExtractor graphics) {
        ClientFlux.get().filter(reading -> reading.stage() != FluxStage.NONE).ifPresent(reading -> {
            Component warning = FluxStageText.styled("codex.thaumory.flux_warning", reading.stage());
            int bandTop = top - 16;
            graphics.fill(left, bandTop, left + WIDTH, top - 2, 0xE0301828);
            graphics.fill(left, top - 3, left + WIDTH, top - 2, FluxStageText.color(reading.stage()));
            graphics.text(font, warning, left + (WIDTH - font.width(warning)) / 2, bandTop + 3, WHITE, true);
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll -= scrollY * LINE * 2;
        return true;
    }

    /** Draws text lines, wrapped to the page, and returns their total height. */
    private int drawLines(GuiGraphicsExtractor graphics, List<Component> lines) {
        int y = contentTop() - (int) scroll;
        int top = y;
        for (Component line : lines) {
            for (FormattedCharSequence wrapped : font.split(line, contentWidth())) {
                graphics.text(font, wrapped, contentLeft(), y, WHITE, true);
                y += LINE;
            }
        }
        return y - top;
    }

    private List<Component> lines(PlayerKnowledge knowledge) {
        return switch (tab) {
            case CHAPTERS -> chapterLines(ClientResearch.get());
            case ASPECTS -> aspectLines(knowledge);
            case CIRCLES -> circleLines(knowledge);
            case HINTS -> hintLines(knowledge);
            case SCANNED -> List.of();
        };
    }

    /** Each open or complete chapter: its title, text, conditions and what it unlocks; then how many are still closed. */
    private static List<Component> chapterLines(ResearchView view) {
        List<Component> lines = new ArrayList<>();
        for (ResearchView.ChapterView chapter : view.chapters()) {
            lines.add(Component.translatable(chapter.complete() ? "codex.thaumory.chapter.complete" : "codex.thaumory.chapter.open",
                    Component.translatable(Chapter.titleKey(chapter.id()))).withColor(chapter.complete() ? 0xFFE8C87A : WHITE));
            lines.add(Component.translatable(Chapter.textKey(chapter.id())).withColor(0xFFCFC6B8));
            for (ResearchView.ConditionLine condition : chapter.conditions()) {
                lines.add(Component.translatable(condition.met() ? "codex.thaumory.condition.met" : "codex.thaumory.condition.unmet",
                        condition.text()).withColor(condition.met() ? 0xFF77DD77 : GRAY));
            }
            if (!chapter.unlocks().isEmpty()) {
                MutableComponent items = Component.empty();
                for (int i = 0; i < chapter.unlocks().size(); i++) {
                    if (i > 0) {
                        items.append(", ");
                    }
                    Identifier item = chapter.unlocks().get(i);
                    items.append(BuiltInRegistries.ITEM.getOptional(item)
                            .map(found -> found.getDefaultInstance().getHoverName())
                            .orElseGet(() -> Component.literal(item.toString())));
                }
                lines.add(Component.translatable("codex.thaumory.chapter.unlocks", items).withColor(0xFFB9A4E0));
            }
            lines.add(Component.empty());
        }
        if (view.closed() > 0) {
            lines.add(Component.translatable("codex.thaumory.chapters.closed", view.closed()).withColor(GRAY));
        }
        return lines;
    }

    private static List<Component> hintLines(PlayerKnowledge knowledge) {
        if (knowledge.hints().isEmpty()) {
            return List.of(Component.translatable("codex.thaumory.hints.empty").withColor(GRAY));
        }
        List<Component> lines = new ArrayList<>();
        knowledge.hints().stream().sorted().forEach(id -> {
            lines.add(Component.translatable(Hint.textKey(id)).withColor(0xFFCFC6B8));
            lines.add(Component.empty());
        });
        return lines;
    }

    private static List<Component> aspectLines(PlayerKnowledge knowledge) {
        List<Aspect> all = List.copyOf(ThaumoryApi.aspects().all());
        long known = all.stream().filter(aspect -> knowledge.knowsAspect(aspect.id())).count();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("codex.thaumory.aspects.count", known, all.size()).withColor(GRAY));
        for (Aspect aspect : all) {
            boolean knows = knowledge.knowsAspect(aspect.id());
            MutableComponent line = AspectText.name(aspect, knows);
            if (knows && !aspect.isPrimal()) {
                List<Aspect> parts = aspect.components();
                line.append(Component.literal(" = ").withColor(GRAY))
                        .append(AspectText.name(parts.get(0), knowledge.knowsAspect(parts.get(0).id())))
                        .append(Component.literal(" + ").withColor(GRAY))
                        .append(AspectText.name(parts.get(1), knowledge.knowsAspect(parts.get(1).id())));
            }
            lines.add(line);
        }
        return lines;
    }

    private static List<Component> circleLines(PlayerKnowledge knowledge) {
        if (knowledge.circles().isEmpty()) {
            return List.of(Component.translatable("codex.thaumory.circles.empty").withColor(GRAY));
        }
        List<Component> lines = new ArrayList<>();
        knowledge.circles().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(CircleCombination::toString)))
                .forEach(entry -> {
                    CircleCombination combination = entry.getKey();
                    MutableComponent line = aspectName(combination.first(), knowledge)
                            .append(Component.literal(" + ").withColor(GRAY))
                            .append(aspectName(combination.second(), knowledge));
                    combination.parameter().ifPresent(parameter -> line
                            .append(Component.literal(" / ").withColor(GRAY))
                            .append(aspectName(parameter, knowledge)));
                    boolean success = entry.getValue() == CircleOutcome.SUCCESS;
                    line.append(Component.literal("  "))
                            .append(Component.translatable(success ? "codex.thaumory.circles.success" : "codex.thaumory.circles.failure")
                                    .withColor(success ? 0xFF77DD77 : 0xFFDD7777));
                    lines.add(line);
                });
        return lines;
    }

    private static MutableComponent aspectName(Identifier id, PlayerKnowledge knowledge) {
        Optional<Aspect> aspect = ThaumoryApi.aspects().get(id);
        return aspect.map(a -> AspectText.name(a, knowledge.knowsAspect(id))).orElseGet(() -> Component.literal(id.toString()));
    }

    /** Draws scanned items as a grid of icons with their tooltip on hover, and returns the grid's height. */
    private int drawScanned(GuiGraphicsExtractor graphics, PlayerKnowledge knowledge, int mouseX, int mouseY) {
        List<ItemStack> stacks = knowledge.scanned(PlayerKnowledge.ITEMS).stream().sorted()
                .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id).stream())
                .map(ItemStack::new)
                .toList();
        graphics.text(font, Component.translatable("codex.thaumory.scanned.count", stacks.size()).withColor(GRAY),
                contentLeft(), contentTop() - (int) scroll, WHITE, true);

        int columns = Math.max(1, contentWidth() / ICON);
        int gridTop = contentTop() + LINE + 2 - (int) scroll;
        boolean mouseInside = mouseX >= contentLeft() && mouseX < contentLeft() + contentWidth()
                && mouseY >= contentTop() && mouseY < contentTop() + contentHeight();
        for (int i = 0; i < stacks.size(); i++) {
            int x = contentLeft() + (i % columns) * ICON;
            int y = gridTop + (i / columns) * ICON;
            graphics.item(stacks.get(i), x, y);
            if (mouseInside && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                graphics.setTooltipForNextFrame(font, stacks.get(i), mouseX, mouseY);
            }
        }
        int rows = (stacks.size() + columns - 1) / columns;
        return LINE + 2 + rows * ICON;
    }
}
