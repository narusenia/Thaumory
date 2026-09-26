package one.nxeu.thaumory.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.wand.WandBuild;
import one.nxeu.thaumory.wand.WandDisplay;
import one.nxeu.thaumory.wand.WandFocus;
import one.nxeu.thaumory.wand.WandPart;
import one.nxeu.thaumory.wand.WandParts;

/**
 * While the player holds a wand, at the top left (requirements §17.7): its focus, and a bar for each
 * aspect it holds, as full as its caps allow. The focus's aspects come first, framed, red when one
 * cast's worth is missing.
 */
final class WandHud {
    private static final int LEFT = 4;
    private static final int TOP = 4;
    private static final int FOCUS_CELL = 22;
    private static final int BAR_WIDTH = 6;
    private static final int BAR_HEIGHT = 36;
    private static final int COLUMN = 16;
    private static final int ICON = 10;
    private static final int PLAIN_FRAME = 0xFF505060;
    private static final int USED_FRAME = 0xFFF0D890;
    private static final int SHORT_FRAME = 0xFFE04040;

    private WandHud() {}

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }
        ItemStack wand = player.getMainHandItem().is(ThaumoryItems.WAND.get()) ? player.getMainHandItem() : player.getOffhandItem();
        if (!wand.is(ThaumoryItems.WAND.get())) {
            return;
        }
        WandBuild build = wand.getOrDefault(ThaumoryComponents.WAND_BUILD.get(), WandBuild.DEFAULT);
        int capacity = WandParts.cap(ClientWandParts.get(), build.cap()).map(WandPart.Cap::essentia).orElse(0);
        Optional<Identifier> focusItem = Optional.ofNullable(wand.get(ThaumoryComponents.WAND_FOCUS.get()));
        Map<Identifier, Integer> cost = focusItem.map(item -> ClientWandParts.foci().get(item)).map(WandFocus::cost).orElse(Map.of());
        Map<Identifier, Integer> stored = new HashMap<>();
        for (AspectStack stack : wand.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty()).stacks()) {
            stored.put(stack.aspect().id(), stack.amount());
        }

        graphics.fill(LEFT, TOP, LEFT + FOCUS_CELL, TOP + FOCUS_CELL, 0x80101018);
        graphics.outline(LEFT, TOP, FOCUS_CELL, FOCUS_CELL, PLAIN_FRAME);
        focusItem.flatMap(BuiltInRegistries.ITEM::getOptional).ifPresent(item -> graphics.item(new ItemStack(item), LEFT + 3, TOP + 3));

        List<WandDisplay.Bar> bars = WandDisplay.bars(cost, stored);
        int x = LEFT + FOCUS_CELL + 6;
        if (bars.isEmpty()) {
            graphics.text(minecraft.font, Component.translatable("hud.thaumory.wand.capacity", capacity), x, TOP + 7, 0xFFAAAAAA, true);
            return;
        }
        PlayerKnowledge knowledge = ClientKnowledge.get();
        for (WandDisplay.Bar bar : bars) {
            Optional<Aspect> aspect = ThaumoryApi.aspects().get(bar.aspect());
            if (aspect.isPresent()) {
                drawBar(graphics, minecraft, x, bar, aspect.get(), capacity, knowledge.knowsAspect(bar.aspect()));
                x += COLUMN;
            }
        }
    }

    private static void drawBar(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, WandDisplay.Bar bar, Aspect aspect, int capacity,
            boolean known) {
        int left = x + (COLUMN - BAR_WIDTH) / 2 - 2;
        graphics.fill(left, TOP, left + BAR_WIDTH, TOP + BAR_HEIGHT, 0x80101018);
        int filled = capacity <= 0 ? 0 : Math.min(BAR_HEIGHT, (int) Math.ceil((double) bar.amount() * BAR_HEIGHT / capacity));
        graphics.fill(left, TOP + BAR_HEIGHT - filled, left + BAR_WIDTH, TOP + BAR_HEIGHT, 0xFF000000 | aspect.color());
        int frame = bar.shortOf() ? SHORT_FRAME : bar.used() ? USED_FRAME : PLAIN_FRAME;
        graphics.outline(left - 1, TOP - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, frame);
        int centre = left + BAR_WIDTH / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, AspectText.iconSprite(aspect, known), centre - ICON / 2, TOP + BAR_HEIGHT + 3,
                ICON, ICON, 0xFF000000 | aspect.color());
        graphics.centeredText(minecraft.font, Integer.toString(bar.amount()), centre, TOP + BAR_HEIGHT + 3 + ICON + 2,
                bar.shortOf() ? 0xFFFF8080 : 0xFFFFFFFF);
    }
}
