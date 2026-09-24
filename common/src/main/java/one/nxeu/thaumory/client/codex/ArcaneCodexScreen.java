package one.nxeu.thaumory.client.codex;

import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import one.nxeu.thaumory.client.CircleText;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.Transcript;
import one.nxeu.thaumory.knowledge.Transcript.AspectTranscript;
import one.nxeu.thaumory.knowledge.Transcript.CircleTranscript;
import one.nxeu.thaumory.network.TranscribePayload;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.knowledge.PlayerKnowledge.CircleOutcome;
import one.nxeu.thaumory.research.Category;
import one.nxeu.thaumory.research.Chapter;
import one.nxeu.thaumory.research.Hint;
import one.nxeu.thaumory.research.ResearchView;

/**
 * The Arcane Codex, drawn as an open book (requirements §7.3): bookmarks along the top edge pick a
 * tab, and the pages turn with the arrows in their bottom corners or the mouse wheel. The chapter
 * tab draws each category's chapters as a tree across the spread, dragged about with the mouse;
 * bookmarks down the left edge pick the category, and a chapter opens as pages of its own. Reads the
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
    /** The chapter tree: grid cells, the node frames in them, and the category bookmarks. */
    private static final int CELL = 30;
    private static final int NODE = 22;
    private static final int TREE_MARGIN = 12;
    private static final int CATEGORY_TAB = 22;
    private static final int LINE_COLOR = 0xFFD8C8A8;
    private static final int LINE_DONE = 0xFFE0B040;
    private static final int TILE = 16;
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

    private static final Component TRANSCRIBE = Component.translatable("codex.thaumory.transcribe");

    private static final Identifier COVER = Thaumory.id("codex/cover");
    private static final Identifier PAGE_LEFT = Thaumory.id("codex/page_left");
    private static final Identifier PAGE_RIGHT = Thaumory.id("codex/page_right");
    private static final Identifier BOOKMARK = Thaumory.id("codex/bookmark");

    /** Remembered while the game runs, so the book reopens where it was left. */
    private static Tab lastTab = Tab.CHAPTERS;
    private static Optional<Identifier> lastCategory = Optional.empty();
    /** The chapter open as pages, if any; the tree shows otherwise. */
    private static Optional<Identifier> lastReading = Optional.empty();
    /** How far each category's tree is dragged, kept while the game runs. */
    private static final Map<Identifier, int[]> PANS = new HashMap<>();

    private Tab tab = lastTab;
    /** Which pair of pages is open: pages {@code 2 * spread} and {@code 2 * spread + 1}. */
    private int spread;
    private Optional<Identifier> reading = lastReading;
    /** Where a press on the tree began, and how far it has dragged since: a short one is a click. */
    private boolean pressedOnTree;
    private double dragged;
    private int left;
    private int top;
    /** What the "transcribe" button would copy: a known aspect or a working circle picked in its tab. */
    private Optional<Transcript> picked = Optional.empty();

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
        picked = Optional.empty();
    }

    private void read(Optional<Identifier> id) {
        reading = lastReading = id;
        spread = 0;
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
            case CHAPTERS -> {
                drawChapters(graphics, ClientResearch.get(), mouseX, mouseY);
                if (reading.isEmpty()) {
                    drawCategoryBookmarks(graphics, ClientResearch.get(), mouseX, mouseY);
                }
            }
            case SCANNED -> drawScanned(graphics, knowledge, mouseX, mouseY);
            case ASPECTS -> drawAspects(graphics, knowledge, mouseX, mouseY);
            case CIRCLES -> drawCircles(graphics, knowledge, mouseX, mouseY);
            case HINTS -> drawPages(graphics, wrap(hintLines(knowledge)));
        }
        drawTranscribeButton(graphics, knowledge, mouseX, mouseY);
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

    /** The category shown: the last one picked if it still has anything to show, or else the first. */
    private Optional<ResearchView.CategoryView> category(ResearchView view) {
        return lastCategory.flatMap(id -> view.categories().stream().filter(c -> c.id().equals(id)).findFirst())
                .or(() -> view.categories().stream().findFirst());
    }

    private void drawChapters(GuiGraphicsExtractor graphics, ResearchView view, int mouseX, int mouseY) {
        Optional<ResearchView.ChapterView> open = reading.flatMap(id -> view.chapters().stream().filter(c -> c.id().equals(id)).findFirst());
        if (open.isPresent()) {
            drawPages(graphics, wrap(chapterLines(open.get())));
            boolean hovered = onBack(mouseX, mouseY);
            graphics.text(font, Component.translatable("codex.thaumory.chapter.back"), textX(1), arrowY(), hovered ? GOLD : FADED, false);
            return;
        }
        reading = lastReading = Optional.empty();
        category(view).ifPresent(shown -> drawTree(graphics, view, shown, mouseX, mouseY));
    }

    // The tree fills the spread inside the pages' margins.
    private int treeLeft() {
        return pageX(0) + 4;
    }

    private int treeTop() {
        return pageY() + 4;
    }

    private int treeRight() {
        return pageX(1) + PAGE_WIDTH - 4;
    }

    private int treeBottom() {
        return pageY() + PAGE_HEIGHT - 4;
    }

    private static List<ResearchView.Node> nodesIn(ResearchView view, Identifier category) {
        List<ResearchView.Node> nodes = new ArrayList<>();
        view.chapters().stream().map(ResearchView.ChapterView::node).filter(n -> n.category().equals(category)).forEach(nodes::add);
        view.unknown().stream().filter(n -> n.category().equals(category)).forEach(nodes::add);
        return nodes;
    }

    /**
     * How far the tree is dragged, kept inside the bounds that leave some of it in view. A tree
     * narrower or shorter than the spread stays centred that way.
     */
    private int[] pan(ResearchView view, Identifier category) {
        List<ResearchView.Node> nodes = nodesIn(view, category);
        int minX = nodes.stream().mapToInt(ResearchView.Node::x).min().orElse(0) * CELL;
        int maxX = nodes.stream().mapToInt(ResearchView.Node::x).max().orElse(0) * CELL + NODE;
        int minY = nodes.stream().mapToInt(ResearchView.Node::y).min().orElse(0) * CELL;
        int maxY = nodes.stream().mapToInt(ResearchView.Node::y).max().orElse(0) * CELL + NODE;
        int[] pan = PANS.computeIfAbsent(category, c -> new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE});
        pan[0] = fit(pan[0], minX, maxX, treeRight() - treeLeft());
        pan[1] = fit(pan[1], minY, maxY, treeBottom() - treeTop());
        return pan;
    }

    private static int fit(int pan, int min, int max, int room) {
        if (max - min <= room - TREE_MARGIN * 2) {
            return (room - (max - min)) / 2 - min;
        }
        int low = room - TREE_MARGIN - max;
        int high = TREE_MARGIN - min;
        return pan == Integer.MIN_VALUE ? high : Math.clamp(pan, low, high);
    }

    private int nodeX(ResearchView.Node node, int[] pan) {
        return treeLeft() + pan[0] + node.x() * CELL;
    }

    private int nodeY(ResearchView.Node node, int[] pan) {
        return treeTop() + pan[1] + node.y() * CELL;
    }

    private void drawTree(GuiGraphicsExtractor graphics, ResearchView view, ResearchView.CategoryView shown, int mouseX, int mouseY) {
        int[] pan = pan(view, shown.id());
        Map<Identifier, ResearchView.ChapterView> byId = new HashMap<>();
        view.chapters().forEach(each -> byId.put(each.id(), each));
        List<ResearchView.Node> nodes = nodesIn(view, shown.id());
        boolean inTree = mouseX >= treeLeft() && mouseX < treeRight() && mouseY >= treeTop() && mouseY < treeBottom();

        graphics.enableScissor(treeLeft(), treeTop(), treeRight(), treeBottom());
        drawBackground(graphics, shown, pan);
        for (ResearchView.Node node : nodes) {
            for (Identifier parent : node.requires()) {
                ResearchView.ChapterView from = byId.get(parent);
                if (from != null && from.node().category().equals(shown.id())) {
                    boolean done = from.complete() && view.chapters().stream().anyMatch(c -> c.node() == node && c.complete());
                    drawLink(graphics, from.node(), node, pan, done ? LINE_DONE : LINE_COLOR);
                }
            }
        }
        Optional<Component> tooltip = Optional.empty();
        for (ResearchView.Node node : nodes) {
            int x = nodeX(node, pan);
            int y = nodeY(node, pan);
            Optional<ResearchView.ChapterView> chapter = view.chapters().stream().filter(c -> c.node() == node).findFirst();
            boolean hovered = inTree && mouseX >= x && mouseX < x + NODE && mouseY >= y && mouseY < y + NODE;
            if (chapter.isPresent()) {
                boolean complete = chapter.get().complete();
                graphics.fill(x, y, x + NODE, y + NODE, hovered ? 0xFFF6EDD2 : 0xFFEADBB8);
                graphics.outline(x, y, NODE, NODE, complete ? LINE_DONE : INK);
                if (complete) {
                    graphics.outline(x + 1, y + 1, NODE - 2, NODE - 2, LINE_DONE);
                }
                Identifier icon = chapter.get().icon();
                BuiltInRegistries.ITEM.getOptional(icon).ifPresent(item -> graphics.item(new ItemStack(item), x + 3, y + 3));
                if (hovered) {
                    Component title = Component.translatable(Chapter.titleKey(chapter.get().id()));
                    tooltip = Optional.of(complete ? Component.translatable("codex.thaumory.chapter.complete", title) : title);
                }
            } else {
                graphics.fill(x, y, x + NODE, y + NODE, 0x40EADBB8);
                graphics.outline(x, y, NODE, NODE, 0x807A6650);
                graphics.text(font, "?", x + (NODE - font.width("?")) / 2, y + 7, FADED, false);
                if (hovered) {
                    tooltip = Optional.of(Component.translatable("codex.thaumory.chapter.unknown"));
                }
            }
        }
        graphics.disableScissor();
        tooltip.ifPresent(text -> graphics.setTooltipForNextFrame(font, text, mouseX, mouseY));
    }

    /** The category's tiles over the whole tree area, moving with the tree, a little darkened; a thin frame round them. */
    private void drawBackground(GuiGraphicsExtractor graphics, ResearchView.CategoryView shown, int[] pan) {
        Identifier texture = shown.background().withPath(path -> "textures/" + path + ".png");
        int startX = treeLeft() + Math.floorMod(pan[0], TILE) - TILE;
        int startY = treeTop() + Math.floorMod(pan[1], TILE) - TILE;
        for (int x = startX; x < treeRight(); x += TILE) {
            for (int y = startY; y < treeBottom(); y += TILE) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, TILE, TILE, TILE, TILE);
            }
        }
        graphics.fill(treeLeft(), treeTop(), treeRight(), treeBottom(), 0x40000000);
        graphics.outline(treeLeft(), treeTop(), treeRight() - treeLeft(), treeBottom() - treeTop(), 0xFF3A2A1C);
    }

    /** Across from the parent's right side, then down or up, then on to the child's left side. */
    private void drawLink(GuiGraphicsExtractor graphics, ResearchView.Node from, ResearchView.Node to, int[] pan, int color) {
        int x0 = nodeX(from, pan) + NODE;
        int y0 = nodeY(from, pan) + NODE / 2;
        int x1 = nodeX(to, pan);
        int y1 = nodeY(to, pan) + NODE / 2;
        if (x1 <= x0) {
            // A child beside or behind its parent: straight from the parent's foot.
            int xs = nodeX(from, pan) + NODE / 2;
            graphics.fill(xs, nodeY(from, pan) + NODE, xs + 1, y1 + 1, color);
            graphics.fill(Math.min(xs, x1 + NODE), y1, Math.max(xs, x1 + NODE) + 1, y1 + 1, color);
            return;
        }
        int middle = (x0 + x1) / 2;
        graphics.fill(x0, y0, middle + 1, y0 + 1, color);
        graphics.fill(middle, Math.min(y0, y1), middle + 1, Math.max(y0, y1) + 1, color);
        graphics.fill(middle, y1, x1, y1 + 1, color);
    }

    /** The chapter under the cursor in the tree; "?" chapters cannot be opened. */
    private Optional<Identifier> chapterAt(ResearchView view, double mouseX, double mouseY) {
        Optional<ResearchView.CategoryView> shown = category(view);
        if (shown.isEmpty() || mouseX < treeLeft() || mouseX >= treeRight() || mouseY < treeTop() || mouseY >= treeBottom()) {
            return Optional.empty();
        }
        int[] pan = pan(view, shown.get().id());
        return view.chapters().stream()
                .filter(c -> c.node().category().equals(shown.get().id()))
                .filter(c -> mouseX >= nodeX(c.node(), pan) && mouseX < nodeX(c.node(), pan) + NODE
                        && mouseY >= nodeY(c.node(), pan) && mouseY < nodeY(c.node(), pan) + NODE)
                .map(ResearchView.ChapterView::id)
                .findFirst();
    }

    private boolean onBack(double x, double y) {
        Component back = Component.translatable("codex.thaumory.chapter.back");
        return x >= textX(1) - 2 && x < textX(1) + font.width(back) + 2 && y >= arrowY() - 2 && y < arrowY() + 10;
    }

    // The category bookmarks stick out of the book's left edge.
    private int categoryX(boolean selected) {
        return left - CATEGORY_TAB + (selected ? -3 : 1);
    }

    private int categoryY(int index) {
        return top + 10 + index * (CATEGORY_TAB + 2);
    }

    private void drawCategoryBookmarks(GuiGraphicsExtractor graphics, ResearchView view, int mouseX, int mouseY) {
        Optional<ResearchView.CategoryView> shown = category(view);
        for (int i = 0; i < view.categories().size(); i++) {
            ResearchView.CategoryView each = view.categories().get(i);
            boolean selected = shown.filter(each::equals).isPresent();
            int x = categoryX(selected);
            int y = categoryY(i);
            graphics.fill(x, y, left + 2, y + CATEGORY_TAB, selected ? Tab.CHAPTERS.color : darker(Tab.CHAPTERS.color));
            graphics.outline(x, y, left + 2 - x, CATEGORY_TAB, 0xFF3A2A1C);
            BuiltInRegistries.ITEM.getOptional(each.icon()).ifPresent(item -> graphics.item(new ItemStack(item), x + 3, y + 3));
            if (mouseX >= x && mouseX < left && mouseY >= y && mouseY < y + CATEGORY_TAB) {
                graphics.setTooltipForNextFrame(font, Component.translatable(Category.nameKey(each.id())), mouseX, mouseY);
            }
        }
    }

    private Optional<ResearchView.CategoryView> categoryAt(ResearchView view, double mouseX, double mouseY) {
        for (int i = 0; i < view.categories().size(); i++) {
            int y = categoryY(i);
            if (mouseX >= categoryX(true) && mouseX < left && mouseY >= y && mouseY < y + CATEGORY_TAB) {
                return Optional.of(view.categories().get(i));
            }
        }
        return Optional.empty();
    }

    /** The open chapter's title, text, conditions and what it unlocks. */
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
                boolean isPicked = picked.equals(Optional.of(new AspectTranscript(aspect.id())));
                if (isPicked || hovered) {
                    graphics.fill(x - 2, y - 2, x + ASPECT_ICON + 2, y + ASPECT_ICON + 2, isPicked ? 0x60806040 : 0x30806040);
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

    /** One wrapped line of the circles tab, with the combination it shows if that one can be copied. */
    private record CircleRow(FormattedCharSequence text, Optional<CircleCombination> copyable) {}

    private List<CircleRow> circleRows(PlayerKnowledge knowledge) {
        if (knowledge.circles().isEmpty()) {
            return List.of(new CircleRow(Component.translatable("codex.thaumory.circles.empty").withColor(FADED).getVisualOrderText(),
                    Optional.empty()));
        }
        List<CircleRow> rows = new ArrayList<>();
        knowledge.circles().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(CircleCombination::toString)))
                .forEach(entry -> {
                    boolean success = entry.getValue() == CircleOutcome.SUCCESS;
                    MutableComponent line = CircleText.combination(entry.getKey(), knowledge, FADED)
                            .append(Component.literal("  "))
                            .append(Component.translatable(success ? "codex.thaumory.circles.success" : "codex.thaumory.circles.failure")
                                    .withColor(success ? MET : UNMET_FAIL));
                    Optional<CircleCombination> copyable = success ? Optional.of(entry.getKey()) : Optional.empty();
                    font.split(line, TEXT_WIDTH).forEach(text -> rows.add(new CircleRow(text, copyable)));
                });
        return rows;
    }

    /** Tried combinations flowing across the pages like text; a working one can be picked to copy. */
    private void drawCircles(GuiGraphicsExtractor graphics, PlayerKnowledge knowledge, int mouseX, int mouseY) {
        List<CircleRow> rows = circleRows(knowledge);
        int pages = Math.max(1, (rows.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        spread = Math.clamp(spread, 0, (pages - 1) / 2);
        Optional<CircleCombination> hovered = circleAt(rows, mouseX, mouseY);
        for (int side = 0; side < 2; side++) {
            int page = spread * 2 + side;
            for (int row = 0; row < LINES_PER_PAGE; row++) {
                int index = page * LINES_PER_PAGE + row;
                if (index >= rows.size()) {
                    break;
                }
                CircleRow each = rows.get(index);
                int y = textY() + row * LINE;
                boolean isPicked = each.copyable().map(CircleTranscript::new).filter(t -> picked.equals(Optional.of(t))).isPresent();
                if (isPicked || (each.copyable().isPresent() && each.copyable().equals(hovered))) {
                    graphics.fill(textX(side) - 2, y - 1, textX(side) + TEXT_WIDTH, y + LINE - 1, isPicked ? 0x40806040 : 0x20806040);
                }
                graphics.text(font, each.text(), textX(side), y, INK, false);
            }
        }
        drawArrows(graphics, spread > 0, spread * 2 + 2 < pages);
    }

    /** The copyable combination on the line under the cursor. */
    private Optional<CircleCombination> circleAt(List<CircleRow> rows, double x, double y) {
        int row = (int) Math.floor((y - textY() + 1) / LINE);
        if (row < 0 || row >= LINES_PER_PAGE) {
            return Optional.empty();
        }
        for (int side = 0; side < 2; side++) {
            if (x >= textX(side) - 2 && x < textX(side) + TEXT_WIDTH) {
                int index = (spread * 2 + side) * LINES_PER_PAGE + row;
                return index < rows.size() ? rows.get(index).copyable() : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** The known aspect under the cursor in the aspects tab. */
    private Optional<Aspect> aspectAt(PlayerKnowledge knowledge, double mouseX, double mouseY) {
        List<Aspect> all = List.copyOf(ThaumoryApi.aspects().all());
        int columns = TEXT_WIDTH / ASPECT_CELL;
        int rows = (PAGE_HEIGHT - PAD * 2 - LINE * 2) / ASPECT_CELL;
        int perPage = columns * rows;
        int gridTop = textY() + LINE + 2;
        for (int side = 0; side < 2; side++) {
            int start = (spread * 2 + side) * perPage;
            for (int i = 0; i < perPage && start + i < all.size(); i++) {
                int x = textX(side) + (i % columns) * ASPECT_CELL + (ASPECT_CELL - ASPECT_ICON) / 2;
                int y = gridTop + (i / columns) * ASPECT_CELL;
                if (mouseX >= x && mouseX < x + ASPECT_ICON && mouseY >= y && mouseY < y + ASPECT_ICON) {
                    Aspect aspect = all.get(start + i);
                    return knowledge.knowsAspect(aspect.id()) ? Optional.of(aspect) : Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    // Transcribing

    private int transcribeX() {
        return textX(1);
    }

    private int transcribeWidth() {
        return font.width(TRANSCRIBE) + 4;
    }

    private boolean onTranscribe(double x, double y) {
        return picked.isPresent() && x >= transcribeX() - 2 && x < transcribeX() + transcribeWidth()
                && y >= arrowY() - 2 && y < arrowY() + 10;
    }

    /** Shown at the foot of the right page while something copyable is picked. */
    private void drawTranscribeButton(GuiGraphicsExtractor graphics, PlayerKnowledge knowledge, int mouseX, int mouseY) {
        if (picked.isEmpty() || !picked.get().knownBy(knowledge)) {
            picked = Optional.empty();
            return;
        }
        boolean hovered = onTranscribe(mouseX, mouseY);
        if (hovered) {
            graphics.fill(transcribeX() - 2, arrowY() - 2, transcribeX() + transcribeWidth(), arrowY() + 10, 0x30806040);
            graphics.setTooltipForNextFrame(font, Component.translatable("codex.thaumory.transcribe.cost"), mouseX, mouseY);
        }
        graphics.text(font, TRANSCRIBE, transcribeX(), arrowY(), hovered ? GOLD : UNLOCK, false);
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
            if (reading.isPresent()) {
                if (onBack(x, y)) {
                    read(Optional.empty());
                    return true;
                }
                if (onLeftArrow) {
                    spread--;
                    return true;
                }
                if (onRightArrow) {
                    spread++;
                    return true;
                }
            } else {
                Optional<ResearchView.CategoryView> picked = categoryAt(view, x, y);
                if (picked.isPresent()) {
                    lastCategory = Optional.of(picked.get().id());
                    return true;
                }
                if (x >= treeLeft() && x < treeRight() && y >= treeTop() && y < treeBottom()) {
                    pressedOnTree = true;
                    dragged = 0;
                    return true;
                }
            }
        } else if (onTranscribe(x, y)) {
            NetworkManager.sendToServer(new TranscribePayload(picked.get()));
            return true;
        } else if (onLeftArrow) {
            spread--;
            return true;
        } else if (tab == Tab.ASPECTS && aspectAt(ClientKnowledge.get(), x, y).isPresent()) {
            pick(new AspectTranscript(aspectAt(ClientKnowledge.get(), x, y).get().id()));
            return true;
        } else if (tab == Tab.CIRCLES && circleAt(circleRows(ClientKnowledge.get()), x, y).isPresent()) {
            pick(new CircleTranscript(circleAt(circleRows(ClientKnowledge.get()), x, y).get()));
            return true;
        } else if (onRightArrow) {
            spread++;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!pressedOnTree) {
            return super.mouseDragged(event, dx, dy);
        }
        dragged += Math.abs(dx) + Math.abs(dy);
        category(ClientResearch.get()).ifPresent(shown -> {
            int[] pan = pan(ClientResearch.get(), shown.id());
            pan[0] += (int) Math.round(dx);
            pan[1] += (int) Math.round(dy);
        });
        return true;
    }

    /** A press on the tree that hardly moved opens the chapter it was on. */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!pressedOnTree) {
            return super.mouseReleased(event);
        }
        pressedOnTree = false;
        if (dragged < 3) {
            Optional<Identifier> clicked = chapterAt(ClientResearch.get(), event.x(), event.y());
            if (clicked.isPresent()) {
                read(clicked);
            }
        }
        return true;
    }

    /** Picking what is already picked lets it go. */
    private void pick(Transcript transcript) {
        picked = picked.equals(Optional.of(transcript)) ? Optional.empty() : Optional.of(transcript);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = scrollY > 0 ? -1 : 1;
        if (tab == Tab.CHAPTERS && reading.isEmpty()) {
            // The wheel moves the tree up and down.
            category(ClientResearch.get()).ifPresent(shown -> pan(ClientResearch.get(), shown.id())[1] -= step * CELL / 2);
        } else {
            spread += step;
        }
        return true;
    }
}
