package one.nxeu.thaumory.client.codex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.flux.FluxStage;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.client.ClientFlux;
import one.nxeu.thaumory.client.ClientKnowledge;
import one.nxeu.thaumory.client.ClientResearch;
import one.nxeu.thaumory.client.FluxStageText;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.knowledge.PlayerKnowledge.CircleOutcome;
import one.nxeu.thaumory.research.Chapter;
import one.nxeu.thaumory.research.Hint;
import one.nxeu.thaumory.research.ResearchView;

/**
 * The Arcane Codex, drawn as an open book (requirements §7.3): bookmarks along the top edge pick a
 * tab, and the pages turn with the arrows in their bottom corners or the mouse wheel. On the
 * chapter tab the left page lists the chapters and the right page shows the one picked. Reads the
 * client's copy of the player's knowledge and research.
 */
public final class ArcaneCodexScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 190;
    private static final int PAGE_X = 10;
    private static final int PAGE_Y = 8;
    private static final int SPINE = 6;
    private static final int PAGE_WIDTH = (WIDTH - PAGE_X * 2 - SPINE) / 2;
    private static final int PAGE_HEIGHT = HEIGHT - PAGE_Y * 2;
    private static final int PAD = 8;
    private static final int TEXT_WIDTH = PAGE_WIDTH - PAD * 2;
    private static final int LINE = 10;
    /** Rows of text on a page, leaving the bottom line for the page arrows. */
    private static final int LINES_PER_PAGE = (PAGE_HEIGHT - PAD * 2 - LINE) / LINE;
    private static final int ICON = 18;
    /** Aspect icons in the aspects tab, drawn at three times the text size. */
    private static final int ASPECT_ICON = 24;
    private static final int ASPECT_CELL = 29;
    private static final int BOOKMARK_HEIGHT = 18;

    private static final int INK = 0xFF3A2A1C;
    private static final int FADED = 0xFF7A6650;
    private static final int GOLD = 0xFF8A5A10;
    private static final int MET = 0xFF2E6E2E;
    private static final int UNMET_FAIL = 0xFF8A2E2E;
    private static final int UNLOCK = 0xFF5A3A8A;

    private static final Identifier COVER = Thaumory.id("codex/cover");
    private static final Identifier PAGE_LEFT = Thaumory.id("codex/page_left");
    private static final Identifier PAGE_RIGHT = Thaumory.id("codex/page_right");
    private static final Identifier BOOKMARK = Thaumory.id("codex/bookmark");

    /** Remembered while the game runs, so the book reopens where it was left. */
    private static Tab lastTab = Tab.CHAPTERS;
    private static Optional<Identifier> lastChapter = Optional.empty();

    private Tab tab = lastTab;
    /** Which pair of pages is open: pages {@code 2 * spread} and {@code 2 * spread + 1}. */
    private int spread;
    private Optional<Identifier> chapter = lastChapter;
    /** Which page of the chosen chapter the right page shows. */
    private int chapterPage;
    private int left;
    private int top;

    private enum Tab {
        CHAPTERS(0xFFB0453A), ASPECTS(0xFF4A7AB0), SCANNED(0xFF5A9A4A), CIRCLES(0xFF8A5AB0), HINTS(0xFFC09A3A);

        final int color;

        Tab(int color) {
            this.color = color;
        }

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
        top = (height - HEIGHT) / 2 + BOOKMARK_HEIGHT / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void select(Tab selected) {
        tab = lastTab = selected;
        spread = 0;
    }

    private void choose(Identifier id) {
        chapter = lastChapter = Optional.of(id);
        chapterPage = 0;
    }

    // Layout

    private int pageX(int side) {
        return left + PAGE_X + side * (PAGE_WIDTH + SPINE);
    }

    private int pageY() {
        return top + PAGE_Y;
    }

    private int textX(int side) {
        return pageX(side) + PAD;
    }

    private int textY() {
        return pageY() + PAD;
    }

    private int arrowY() {
        return pageY() + PAGE_HEIGHT - PAD - 8;
    }

    private int bookmarkX(Tab each) {
        int x = left + 14;
        for (Tab before : Tab.values()) {
            if (before == each) {
                return x;
            }
            x += bookmarkWidth(before) + 2;
        }
        return x;
    }

    private int bookmarkWidth(Tab each) {
        return font.width(each.title()) + 12;
    }

    private int bookmarkY(Tab each) {
        return top - BOOKMARK_HEIGHT + (each == tab ? 2 : 6);
    }

    // Drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (Tab each : Tab.values()) {
            int color = each == tab ? each.color : darker(each.color);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BOOKMARK, bookmarkX(each), bookmarkY(each), bookmarkWidth(each),
                    BOOKMARK_HEIGHT + 4, color);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, COVER, left, top, WIDTH, HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_LEFT, pageX(0), pageY(), PAGE_WIDTH, PAGE_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_RIGHT, pageX(1), pageY(), PAGE_WIDTH, PAGE_HEIGHT);
    }

    private static int darker(int color) {
        return 0xFF000000 | (((color >> 16) & 0xFF) * 3 / 5) << 16 | (((color >> 8) & 0xFF) * 3 / 5) << 8 | (color & 0xFF) * 3 / 5;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (Tab each : Tab.values()) {
            graphics.text(font, each.title(), bookmarkX(each) + 6, bookmarkY(each) + 4, 0xFFFFFFFF, true);
        }
        PlayerKnowledge knowledge = ClientKnowledge.get();
        switch (tab) {
            case CHAPTERS -> drawChapters(graphics, ClientResearch.get(), mouseX, mouseY);
            case SCANNED -> drawScanned(graphics, knowledge, mouseX, mouseY);
            case ASPECTS -> drawAspects(graphics, knowledge, mouseX, mouseY);
            default -> drawPages(graphics, wrap(lines(knowledge)));
        }
        drawFluxWarning(graphics);
    }

    /** Wraps lines to the page width. */
    private List<FormattedCharSequence> wrap(List<Component> lines) {
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            if (line.getString().isEmpty()) {
                wrapped.add(FormattedCharSequence.EMPTY);
                continue;
            }
            wrapped.addAll(font.split(line, TEXT_WIDTH));
        }
        return wrapped;
    }

    /** Lines flowing from the left page onto the right, a spread at a time. */
    private void drawPages(GuiGraphicsExtractor graphics, List<FormattedCharSequence> lines) {
        int pages = Math.max(1, (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        spread = Math.clamp(spread, 0, (pages - 1) / 2);
        for (int side = 0; side < 2; side++) {
            int page = spread * 2 + side;
            for (int row = 0; row < LINES_PER_PAGE; row++) {
                int index = page * LINES_PER_PAGE + row;
                if (index >= lines.size()) {
                    break;
                }
                graphics.text(font, lines.get(index), textX(side), textY() + row * LINE, INK, false);
            }
        }
        drawArrows(graphics, spread > 0, spread * 2 + 2 < pages);
    }

    private void drawArrows(GuiGraphicsExtractor graphics, boolean back, boolean forward) {
        if (back) {
            graphics.text(font, "◀", textX(0), arrowY(), FADED, false);
        }
        if (forward) {
            graphics.text(font, "▶", pageX(1) + PAGE_WIDTH - PAD - 8, arrowY(), FADED, false);
        }
    }

    private void drawFluxWarning(GuiGraphicsExtractor graphics) {
        ClientFlux.get().filter(reading -> reading.stage() != FluxStage.NONE).ifPresent(reading -> {
            Component warning = FluxStageText.styled("codex.thaumory.flux_warning", reading.stage());
            int bandTop = top + HEIGHT + 2;
            graphics.fill(left, bandTop, left + WIDTH, bandTop + 14, 0xE0301828);
            graphics.fill(left, bandTop, left + WIDTH, bandTop + 1, FluxStageText.color(reading.stage()));
            graphics.text(font, warning, left + (WIDTH - font.width(warning)) / 2, bandTop + 3, 0xFFFFFFFF, true);
        });
    }

    // Chapters

    private Optional<ResearchView.ChapterView> chosen(ResearchView view) {
        Optional<ResearchView.ChapterView> picked = chapter.flatMap(id -> view.chapters().stream().filter(c -> c.id().equals(id)).findFirst());
        if (picked.isPresent()) {
            return picked;
        }
        // The first chapter still open, or else the last one.
        return view.chapters().stream().filter(c -> !c.complete()).findFirst()
                .or(() -> view.chapters().isEmpty() ? Optional.empty() : Optional.of(view.chapters().getLast()));
    }

    private void drawChapters(GuiGraphicsExtractor graphics, ResearchView view, int mouseX, int mouseY) {
        Optional<ResearchView.ChapterView> picked = chosen(view);
        int y = textY();
        for (ResearchView.ChapterView each : view.chapters()) {
            boolean hovered = mouseX >= textX(0) - 2 && mouseX < textX(0) + TEXT_WIDTH && mouseY >= y - 1 && mouseY < y + ICON - 1;
            boolean isPicked = picked.filter(each::equals).isPresent();
            if (isPicked || hovered) {
                graphics.fill(textX(0) - 2, y - 1, textX(0) + TEXT_WIDTH, y + ICON - 1, isPicked ? 0x40806040 : 0x20806040);
            }
            int rowY = y;
            BuiltInRegistries.ITEM.getOptional(each.icon()).ifPresent(item -> graphics.item(new ItemStack(item), textX(0), rowY));
            Component title = Component.translatable(Chapter.titleKey(each.id()));
            String mark = each.complete() ? "✔ " : "";
            graphics.text(font, font.substrByWidth(Component.literal(mark).append(title), TEXT_WIDTH - 20).getString(), textX(0) + 20, y + 4,
                    each.complete() ? GOLD : INK, false);
            y += ICON;
        }
        if (view.closed() > 0) {
            graphics.text(font, Component.translatable("codex.thaumory.chapters.closed", view.closed()), textX(0),
                    pageY() + PAGE_HEIGHT - PAD - 8, FADED, false);
        }

        List<FormattedCharSequence> detail = picked.map(each -> wrap(chapterLines(each))).orElse(List.of());
        int pages = Math.max(1, (detail.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        chapterPage = Math.clamp(chapterPage, 0, pages - 1);
        for (int row = 0; row < LINES_PER_PAGE; row++) {
            int index = chapterPage * LINES_PER_PAGE + row;
            if (index >= detail.size()) {
                break;
            }
            graphics.text(font, detail.get(index), textX(1), textY() + row * LINE, INK, false);
        }
        if (chapterPage > 0) {
            graphics.text(font, "◀", textX(1), arrowY(), FADED, false);
        }
        if (chapterPage + 1 < pages) {
            graphics.text(font, "▶", pageX(1) + PAGE_WIDTH - PAD - 8, arrowY(), FADED, false);
        }
    }

    /** The chosen chapter's title, text, conditions and what it unlocks. */
    private static List<Component> chapterLines(ResearchView.ChapterView chapter) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(Chapter.titleKey(chapter.id())).withColor(chapter.complete() ? GOLD : INK));
        lines.add(Component.empty());
        lines.add(Component.translatable(Chapter.textKey(chapter.id())).withColor(INK));
        if (!chapter.conditions().isEmpty()) {
            lines.add(Component.empty());
            for (ResearchView.ConditionLine condition : chapter.conditions()) {
                lines.add(Component.translatable(condition.met() ? "codex.thaumory.condition.met" : "codex.thaumory.condition.unmet",
                        condition.text()).withColor(condition.met() ? MET : FADED));
            }
        }
        if (!chapter.unlocks().isEmpty()) {
            lines.add(Component.empty());
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
            lines.add(Component.translatable("codex.thaumory.chapter.unlocks", items).withColor(UNLOCK));
        }
        return lines;
    }

    // Other tabs

    private List<Component> lines(PlayerKnowledge knowledge) {
        return switch (tab) {
            case CIRCLES -> circleLines(knowledge);
            case HINTS -> hintLines(knowledge);
            case CHAPTERS, SCANNED, ASPECTS -> List.of();
        };
    }

    private static List<Component> hintLines(PlayerKnowledge knowledge) {
        if (knowledge.hints().isEmpty()) {
            return List.of(Component.translatable("codex.thaumory.hints.empty").withColor(FADED));
        }
        List<Component> lines = new ArrayList<>();
        knowledge.hints().stream().sorted().forEach(id -> {
            lines.add(Component.translatable(Hint.textKey(id)).withColor(INK));
            lines.add(Component.empty());
        });
        return lines;
    }

    /**
     * Every aspect as a large icon, in rows across both pages; hovering one shows its name and
     * makeup. Unknown ones show the shared icon and only their glyphs.
     */
    private void drawAspects(GuiGraphicsExtractor graphics, PlayerKnowledge knowledge, int mouseX, int mouseY) {
        List<Aspect> all = List.copyOf(ThaumoryApi.aspects().all());
        long known = all.stream().filter(aspect -> knowledge.knowsAspect(aspect.id())).count();
        int columns = TEXT_WIDTH / ASPECT_CELL;
        int rows = (PAGE_HEIGHT - PAD * 2 - LINE * 2) / ASPECT_CELL;
        int perPage = columns * rows;
        int pages = Math.max(1, (all.size() + perPage - 1) / perPage);
        spread = Math.clamp(spread, 0, (pages - 1) / 2);
        graphics.text(font, Component.translatable("codex.thaumory.aspects.count", known, all.size()), textX(0), textY(), FADED, false);
        int gridTop = textY() + LINE + 2;
        for (int side = 0; side < 2; side++) {
            int start = (spread * 2 + side) * perPage;
            for (int i = 0; i < perPage && start + i < all.size(); i++) {
                Aspect aspect = all.get(start + i);
                boolean knows = knowledge.knowsAspect(aspect.id());
                int x = textX(side) + (i % columns) * ASPECT_CELL + (ASPECT_CELL - ASPECT_ICON) / 2;
                int y = gridTop + (i / columns) * ASPECT_CELL;
                boolean hovered = mouseX >= x && mouseX < x + ASPECT_ICON && mouseY >= y && mouseY < y + ASPECT_ICON;
                if (hovered) {
                    graphics.fill(x - 2, y - 2, x + ASPECT_ICON + 2, y + ASPECT_ICON + 2, 0x30806040);
                }
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, AspectText.iconSprite(aspect, knows), x, y, ASPECT_ICON, ASPECT_ICON,
                        0xFF000000 | aspect.color());
                if (hovered) {
                    graphics.setTooltipForNextFrame(aspectTooltip(aspect, knowledge).stream().map(Component::getVisualOrderText).toList(),
                            mouseX, mouseY);
                }
            }
        }
        drawArrows(graphics, spread > 0, spread * 2 + 2 < pages);
    }

    private static List<Component> aspectTooltip(Aspect aspect, PlayerKnowledge knowledge) {
        boolean knows = knowledge.knowsAspect(aspect.id());
        List<Component> lines = new ArrayList<>();
        lines.add(AspectText.name(aspect, knows));
        if (!knows) {
            lines.add(Component.translatable("codex.thaumory.aspects.unknown").withColor(0xFFAAAAAA));
        } else if (!aspect.isPrimal()) {
            List<Aspect> parts = aspect.components();
            lines.add(Component.literal("= ").withColor(0xFFAAAAAA)
                    .append(AspectText.name(parts.get(0), knowledge.knowsAspect(parts.get(0).id())))
                    .append(Component.literal(" + ").withColor(0xFFAAAAAA))
                    .append(AspectText.name(parts.get(1), knowledge.knowsAspect(parts.get(1).id()))));
        } else {
            lines.add(Component.translatable("codex.thaumory.aspects.primal").withColor(0xFFAAAAAA));
        }
        return lines;
    }

    private static List<Component> circleLines(PlayerKnowledge knowledge) {
        if (knowledge.circles().isEmpty()) {
            return List.of(Component.translatable("codex.thaumory.circles.empty").withColor(FADED));
        }
        List<Component> lines = new ArrayList<>();
        knowledge.circles().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(CircleCombination::toString)))
                .forEach(entry -> {
                    CircleCombination combination = entry.getKey();
                    MutableComponent line = aspectName(combination.first(), knowledge)
                            .append(Component.literal(" + ").withColor(FADED))
                            .append(aspectName(combination.second(), knowledge));
                    combination.parameter().ifPresent(parameter -> line
                            .append(Component.literal(" / ").withColor(FADED))
                            .append(aspectName(parameter, knowledge)));
                    boolean success = entry.getValue() == CircleOutcome.SUCCESS;
                    line.append(Component.literal("  "))
                            .append(Component.translatable(success ? "codex.thaumory.circles.success" : "codex.thaumory.circles.failure")
                                    .withColor(success ? MET : UNMET_FAIL));
                    lines.add(line);
                });
        return lines;
    }

    private static MutableComponent aspectName(Identifier id, PlayerKnowledge knowledge) {
        Optional<Aspect> aspect = ThaumoryApi.aspects().get(id);
        return aspect.map(a -> AspectText.name(a, knowledge.knowsAspect(id))).orElseGet(() -> Component.literal(id.toString()));
    }

    /** Scanned items as a grid of icons across both pages, with their tooltip on hover. */
    private void drawScanned(GuiGraphicsExtractor graphics, PlayerKnowledge knowledge, int mouseX, int mouseY) {
        List<ItemStack> stacks = knowledge.scanned(PlayerKnowledge.ITEMS).stream().sorted()
                .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id).stream())
                .map(ItemStack::new)
                .toList();
        int columns = TEXT_WIDTH / ICON;
        int rows = (PAGE_HEIGHT - PAD * 2 - LINE) / ICON;
        // The first page gives its top row to the count.
        int firstPage = columns * (rows - 1);
        int perPage = columns * rows;
        int pages = stacks.size() <= firstPage ? 1 : 1 + (stacks.size() - firstPage + perPage - 1) / perPage;
        spread = Math.clamp(spread, 0, (pages - 1) / 2);
        if (spread == 0) {
            graphics.text(font, Component.translatable("codex.thaumory.scanned.count", stacks.size()), textX(0), textY() + 4, FADED, false);
        }
        for (int side = 0; side < 2; side++) {
            int page = spread * 2 + side;
            int start = page == 0 ? 0 : firstPage + (page - 1) * perPage;
            int count = page == 0 ? firstPage : perPage;
            int rowOffset = page == 0 ? 1 : 0;
            for (int i = 0; i < count && start + i < stacks.size(); i++) {
                int x = textX(side) + (i % columns) * ICON;
                int y = textY() + (i / columns + rowOffset) * ICON;
                ItemStack stack = stacks.get(start + i);
                graphics.item(stack, x, y);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
                }
            }
        }
        drawArrows(graphics, spread > 0, spread * 2 + 2 < pages);
    }

    // Input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x();
        double y = event.y();
        for (Tab each : Tab.values()) {
            if (x >= bookmarkX(each) && x < bookmarkX(each) + bookmarkWidth(each) && y >= bookmarkY(each) && y < top) {
                select(each);
                return true;
            }
        }
        boolean onArrowRow = y >= arrowY() - 2 && y < arrowY() + 10;
        boolean onLeftArrow = onArrowRow && x >= textX(0) - 2 && x < textX(0) + 10;
        boolean onRightArrow = onArrowRow && x >= pageX(1) + PAGE_WIDTH - PAD - 10 && x < pageX(1) + PAGE_WIDTH - PAD + 2;
        if (tab == Tab.CHAPTERS) {
            ResearchView view = ClientResearch.get();
            int row = (int) Math.floor((y - textY() + 1) / ICON);
            if (x >= textX(0) - 2 && x < textX(0) + TEXT_WIDTH && y >= textY() - 1 && row >= 0 && row < view.chapters().size()) {
                choose(view.chapters().get(row).id());
                return true;
            }
            boolean onDetailBack = onArrowRow && x >= textX(1) - 2 && x < textX(1) + 10;
            if (onDetailBack) {
                chapterPage--;
                return true;
            }
            if (onRightArrow) {
                chapterPage++;
                return true;
            }
        } else if (onLeftArrow) {
            spread--;
            return true;
        } else if (onRightArrow) {
            spread++;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = scrollY > 0 ? -1 : 1;
        if (tab == Tab.CHAPTERS) {
            chapterPage += step;
        } else {
            spread += step;
        }
        return true;
    }
}
