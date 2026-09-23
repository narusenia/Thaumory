package one.nxeu.thaumory.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.block.crucible.CrucibleBlock;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.crucible.CrucibleTank;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** Shows a Crucible's contents beside the crosshair while the player holds the Arcane Loupe and looks at it. */
final class CrucibleHud {
    private static final int GRAY = 0xFFAAAAAA;
    private static final int WHITE = 0xFFFFFFFF;

    private CrucibleHud() {}

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || !holdsLoupe(player)) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof CrucibleBlockEntity crucible)) {
            return;
        }

        CrucibleTank tank = crucible.tank();
        boolean boiling = crucible.getBlockState().getValue(CrucibleBlock.BOILING);
        PlayerKnowledge knowledge = ClientKnowledge.get();

        List<Component> lines = new ArrayList<>();
        lines.add(crucible.getBlockState().getBlock().getName().withColor(WHITE));
        lines.add(Component.translatable("hud.thaumory.crucible.water", tank.water(), CrucibleTank.MAX_WATER).withColor(GRAY)
                .append(" ")
                .append(Component.translatable(boiling ? "hud.thaumory.crucible.boiling" : "hud.thaumory.crucible.not_boiling")
                        .withColor(boiling ? 0xFFFF9955 : GRAY)));
        lines.add(Component.translatable("hud.thaumory.crucible.essentia", tank.contents().total(), crucible.capacity()).withColor(GRAY));
        for (AspectStack stack : tank.contents().sortedByAmount()) {
            lines.add(Component.literal(" ").append(AspectText.stack(stack, knowledge.knowsAspect(stack.aspect().id()))));
        }

        int x = graphics.guiWidth() / 2 + 12;
        int y = graphics.guiHeight() / 2 - lines.size() * 5;
        for (Component line : lines) {
            graphics.text(minecraft.font, line, x, y, WHITE, true);
            y += 10;
        }
    }

    private static boolean holdsLoupe(Player player) {
        return player.getMainHandItem().is(ThaumoryItems.ARCANE_LOUPE.get()) || player.getOffhandItem().is(ThaumoryItems.ARCANE_LOUPE.get());
    }
}
