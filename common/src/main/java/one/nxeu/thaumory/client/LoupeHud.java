package one.nxeu.thaumory.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.api.flux.FluxStage;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.block.core.CoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlock;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.circle.CircleMode;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.crucible.CrucibleTank;
import one.nxeu.thaumory.item.ArcaneLoupeItem;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/**
 * While the player holds the Arcane Loupe: the Flux of the chunk they stand in at the top right,
 * and what a Crucible or a jar holds, or how a circle's Core reads, beside the crosshair.
 */
final class LoupeHud {
    private static final int GRAY = 0xFFAAAAAA;
    private static final int WHITE = 0xFFFFFFFF;

    private LoupeHud() {}

    static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || !ArcaneLoupeItem.isHeldBy(player)) {
            return;
        }
        renderFlux(graphics, minecraft);
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        List<Component> lines = switch (minecraft.level.getBlockEntity(hit.getBlockPos())) {
            case CrucibleBlockEntity crucible -> crucibleLines(crucible);
            case JarBlockEntity jar -> jarLines(jar);
            case CoreBlockEntity core -> coreLines(core);
            case null, default -> List.of();
        };

        int x = graphics.guiWidth() / 2 + 12;
        int y = graphics.guiHeight() / 2 - lines.size() * 5;
        for (Component line : lines) {
            graphics.text(minecraft.font, line, x, y, WHITE, true);
            y += 10;
        }
    }

    /** The last reading, right-aligned in the top right corner: the amount, then the stage in its color. */
    private static void renderFlux(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        ClientFlux.get().ifPresent(reading -> {
            Component amount = Component.translatable("hud.thaumory.flux.amount", (int) Math.floor(reading.amount())).withColor(WHITE);
            Component stage = Component.translatable("hud.thaumory.flux.stage." + reading.stage().name().toLowerCase(Locale.ROOT))
                    .withColor(stageColor(reading.stage()));
            int right = graphics.guiWidth() - 6;
            graphics.text(minecraft.font, amount, right - minecraft.font.width(amount), 6, WHITE, true);
            graphics.text(minecraft.font, stage, right - minecraft.font.width(stage), 16, WHITE, true);
        });
    }

    private static int stageColor(FluxStage stage) {
        return switch (stage) {
            case NONE -> GRAY;
            case STAGNATION -> 0xFFD7A6FF;
            case EROSION -> 0xFFB266FF;
            case MANIFESTATION -> 0xFFE0409A;
            case OVERLOAD -> 0xFFFF4040;
        };
    }

    private static List<Component> jarLines(JarBlockEntity jar) {
        List<Component> lines = new ArrayList<>();
        lines.add(jar.getBlockState().getBlock().getName().withColor(WHITE));
        lines.addAll(JarText.describe(jar.contents(), OptionalInt.of(jar.displayCapacity()), ClientKnowledge.get()));
        return lines;
    }

    /** The circle diagnosis: runes, rings, modifiers on the nodes and off them, and instability. */
    private static List<Component> coreLines(CoreBlockEntity core) {
        PlayerKnowledge knowledge = ClientKnowledge.get();
        CircleScan scan = core.scan();
        List<Component> lines = new ArrayList<>();
        lines.add(core.getBlockState().getBlock().getName().withColor(WHITE));

        MutableComponent runes = Component.translatable("hud.thaumory.core.runes").withColor(GRAY);
        for (int slot = 0; slot < CoreBlockEntity.SLOTS; slot++) {
            runes.append(" ");
            if (slot < core.runes().size()) {
                Identifier id = core.runes().get(slot);
                runes.append(ThaumoryApi.aspects().get(id)
                        .map(aspect -> (Component) AspectText.name(aspect, knowledge.knowsAspect(id)))
                        .orElseGet(() -> Component.literal(id.toString()).withColor(GRAY)));
            } else {
                runes.append(Component.translatable("hud.thaumory.core.empty_slot").withColor(GRAY));
            }
        }
        lines.add(runes);

        lines.add(Component.translatable("hud.thaumory.core.rings", scan.rings(), CircleScan.MAX_RINGS).withColor(GRAY));
        for (CircleScan.Node node : scan.nodes()) {
            Component pattern = BuiltInRegistries.BLOCK.getOptional(node.pattern())
                    .map(block -> (Component) block.getName())
                    .orElseGet(() -> Component.literal(node.pattern().toString()));
            lines.add(Component.literal(" ").append(Component.translatable("hud.thaumory.core.node", node.ring(),
                    Component.translatable("hud.thaumory.core.side." + node.side().name().toLowerCase(Locale.ROOT)), pattern)
                    .withColor(GRAY)));
        }
        if (!scan.ignoredModifiers().isEmpty()) {
            lines.add(Component.translatable("hud.thaumory.core.ignored", scan.ignoredModifiers().size()).withColor(0xFFFFAA55));
        }

        lines.add(Component.translatable(core.isRunning() ? "hud.thaumory.core.running" : "hud.thaumory.core.stopped")
                .withColor(core.isRunning() ? 0xFF55FF88 : GRAY));
        // Name the circle and its cost only once this player has seen it work, so the loupe never gives answers away.
        core.combination().filter(combination -> scan.rings() > 0).ifPresent(combination -> {
            Optional<PlayerKnowledge.CircleOutcome> outcome = knowledge.circle(combination);
            Optional<Identifier> effect = core.effectId();
            if (outcome.filter(PlayerKnowledge.CircleOutcome.SUCCESS::equals).isPresent() && effect.isPresent()) {
                lines.add(Component.translatable("hud.thaumory.core.effect", Component.translatable(effect.get().toLanguageKey("circle_effect")))
                        .withColor(0xFFCC99FF));
                core.upkeep().ifPresent(upkeep -> lines.add((upkeep.mode() == CircleMode.TRIGGERED
                        ? Component.translatable("hud.thaumory.core.upkeep.triggered", upkeep.cost())
                        : Component.translatable("hud.thaumory.core.upkeep.sustained", String.format(Locale.ROOT, "%.1f", upkeep.interval() / 20.0)))
                        .withColor(GRAY)));
            } else if (outcome.filter(PlayerKnowledge.CircleOutcome.FAILURE::equals).isPresent()) {
                lines.add(Component.translatable("hud.thaumory.core.failed_circle").withColor(0xFFFFAA55));
            } else {
                lines.add(Component.translatable("hud.thaumory.core.unknown_circle").withColor(GRAY));
            }
        });
        lines.add(Component.translatable("hud.thaumory.core.essentia", core.displayCapacity()).withColor(GRAY));
        for (AspectStack stack : core.essentia().sortedByAmount()) {
            lines.add(Component.literal(" ").append(AspectText.stack(stack, knowledge.knowsAspect(stack.aspect().id()))));
        }

        int threshold = core.displayThreshold();
        boolean unstable = core.instability() > threshold;
        lines.add(Component.translatable("hud.thaumory.core.instability", core.instability(), threshold)
                .withColor(unstable ? 0xFFFF5555 : GRAY));
        if (unstable) {
            lines.add(Component.translatable("hud.thaumory.core.unstable").withColor(0xFFFF5555));
        }
        return lines;
    }

    private static List<Component> crucibleLines(CrucibleBlockEntity crucible) {
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
        List<List<Aspect>> cancelling = tank.contents().cancellingPairs(ThaumoryApi.aspects());
        if (boiling && !cancelling.isEmpty()) {
            lines.add(Component.translatable("hud.thaumory.crucible.cancelling").withColor(0xFFFF5555));
            for (List<Aspect> pair : cancelling) {
                lines.add(Component.literal(" ")
                        .append(AspectText.name(pair.get(0), knowledge.knowsAspect(pair.get(0).id())))
                        .append(Component.literal(" ⇄ ").withColor(0xFFFF5555))
                        .append(AspectText.name(pair.get(1), knowledge.knowsAspect(pair.get(1).id()))));
            }
        }
        return lines;
    }
}
