package one.nxeu.thaumory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.flux.FluxManager;
import one.nxeu.thaumory.flux.FluxWorldEffects;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.knowledge.PlayerKnowledge.CircleOutcome;
import one.nxeu.thaumory.scan.ItemScanner;

/** Debug commands under {@code /thaumory}. Operators only. */
public final class ThaumoryCommands {
    private static final DynamicCommandExceptionType UNKNOWN_ASPECT =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown aspect: " + id));

    private ThaumoryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, FluxManager flux) {
        dispatcher.register(Commands.literal("thaumory")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("aspects")
                        .then(Commands.literal("report").executes(ThaumoryCommands::writeReport))
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(ThaumoryCommands::showAspects)))
                .then(Commands.literal("flux")
                        .then(atChunk(Commands.literal("get"), (c, chunk) -> showFlux(c, flux, chunk)))
                        .then(Commands.literal("add").then(atChunk(amountArgument(), (c, chunk) -> {
                            flux.add(c.getSource().getLevel(), chunk, DoubleArgumentType.getDouble(c, "amount"));
                            return showFlux(c, flux, chunk);
                        })))
                        .then(Commands.literal("set").then(atChunk(amountArgument(), (c, chunk) -> {
                            flux.set(c.getSource().getLevel(), chunk, DoubleArgumentType.getDouble(c, "amount"));
                            return showFlux(c, flux, chunk);
                        })))
                        .then(Commands.literal("pollute").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> {
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
                            String before = c.getSource().getLevel().getBlockState(pos).toString();
                            boolean polluted = FluxWorldEffects.pollute(c.getSource().getLevel(), pos);
                            c.getSource().sendSuccess(() -> Component.literal("pollute " + pos.toShortString() + " (" + before + "): "
                                    + (polluted ? "polluted" : "no rule")), false);
                            return polluted ? 1 : 0;
                        }))))
                .then(Commands.literal("circle").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ThaumoryCommands::showCircle)
                        .then(Commands.literal("start").executes(c -> withCore(c, core -> "start: " + core.start(Optional.ofNullable(c.getSource().getEntity())))))
                        .then(Commands.literal("stop").executes(c -> withCore(c, core -> "stop: " + (core.stop() ? "stopped" : "not running"))))
                        .then(Commands.literal("trigger").executes(c -> withCore(c,
                                core -> "trigger: " + core.trigger(Optional.ofNullable(c.getSource().getEntity())))))))
                .then(Commands.literal("knowledge")
                        .then(Commands.literal("show").then(Commands.argument("player", EntityArgument.player())
                                .executes(ThaumoryCommands::showKnowledge)))
                        .then(Commands.literal("scan").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("item", ItemArgument.item(context)).executes(c -> {
                                    ItemScanner.scan(EntityArgument.getPlayer(c, "player"), ItemArgument.getItem(c, "item").item().value());
                                    return showKnowledge(c);
                                }))))
                        .then(Commands.literal("reveal").then(Commands.argument("player", EntityArgument.player())
                                .then(aspectArgument("aspect").executes(c -> {
                                    Identifier aspect = aspect(c, "aspect");
                                    return changeKnowledge(c, k -> k.withAspect(aspect));
                                }))))
                        .then(Commands.literal("chapter").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", IdentifierArgument.id()).executes(c -> {
                                    Identifier chapter = IdentifierArgument.getId(c, "id");
                                    return changeKnowledge(c, k -> k.withChapter(chapter));
                                }))))
                        .then(Commands.literal("hint").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("id", IdentifierArgument.id()).executes(c -> {
                                    Identifier hint = IdentifierArgument.getId(c, "id");
                                    return changeKnowledge(c, k -> k.withHint(hint));
                                }))))
                        .then(Commands.literal("circle").then(Commands.argument("player", EntityArgument.player())
                                .then(circleOutcome("success", CircleOutcome.SUCCESS))
                                .then(circleOutcome("failure", CircleOutcome.FAILURE))))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> changeKnowledge(c, k -> PlayerKnowledge.EMPTY))))));
    }

    /** {@code circle <player> success|failure <first> <second> [parameter [slot4]]}. */
    private static ArgumentBuilder<CommandSourceStack, ?> circleOutcome(String name, CircleOutcome outcome) {
        return Commands.literal(name).then(aspectArgument("first").then(aspectArgument("second")
                .executes(c -> recordCircle(c, outcome, Optional.empty(), Optional.empty()))
                .then(aspectArgument("parameter")
                        .executes(c -> recordCircle(c, outcome, Optional.of(aspect(c, "parameter")), Optional.empty()))
                        .then(aspectArgument("slot4")
                                .executes(c -> recordCircle(c, outcome, Optional.of(aspect(c, "parameter")), Optional.of(aspect(c, "slot4"))))))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> aspectArgument(String name) {
        return Commands.argument(name, IdentifierArgument.id()).suggests((c, builder) ->
                SharedSuggestionProvider.suggestResource(ThaumoryApi.aspects().all().stream().map(Aspect::id), builder));
    }

    private static int recordCircle(CommandContext<CommandSourceStack> context, CircleOutcome outcome, Optional<Identifier> parameter,
            Optional<Identifier> slot4) throws CommandSyntaxException {
        CircleCombination combination = new CircleCombination(aspect(context, "first"), aspect(context, "second"), parameter, slot4);
        return changeKnowledge(context, k -> k.withCircle(combination, outcome));
    }

    private static Identifier aspect(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, argument);
        if (ThaumoryApi.aspects().get(id).isEmpty()) {
            throw UNKNOWN_ASPECT.create(id);
        }
        return id;
    }

    private static int changeKnowledge(CommandContext<CommandSourceStack> context, UnaryOperator<PlayerKnowledge> change)
            throws CommandSyntaxException {
        Thaumory.knowledge().update(EntityArgument.getPlayer(context, "player"), change);
        return showKnowledge(context);
    }

    private static int showKnowledge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        PlayerKnowledge knowledge = Thaumory.knowledge().get(player);
        long successes = knowledge.circles().values().stream().filter(outcome -> outcome == CircleOutcome.SUCCESS).count();
        String text = String.format(Locale.ROOT, "%s: %d scanned items, aspects %s, %d circles (%d succeeded), %d chapters, %d hints",
                player.getScoreboardName(), knowledge.scanned(PlayerKnowledge.ITEMS).size(),
                knowledge.aspects().stream().sorted().toList(), knowledge.circles().size(), successes,
                knowledge.chapters().size(), knowledge.hints().size());
        context.getSource().sendSuccess(() -> Component.literal(text), false);
        return knowledge.scanned(PlayerKnowledge.ITEMS).size();
    }

    private interface ChunkCommand {
        int run(CommandContext<CommandSourceStack> context, ChunkPos chunk);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> amountArgument() {
        return Commands.argument("amount", DoubleArgumentType.doubleArg(0));
    }

    /** Runs {@code command} for the executor's chunk, or for the chunk containing an optional block position. */
    private static ArgumentBuilder<CommandSourceStack, ?> atChunk(ArgumentBuilder<CommandSourceStack, ?> node, ChunkCommand command) {
        return node
                .executes(c -> command.run(c, ChunkPos.containing(BlockPos.containing(c.getSource().getPosition()))))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> command.run(c, ChunkPos.containing(BlockPosArgument.getBlockPos(c, "pos")))));
    }

    /** Rescans the Core at {@code pos}, runs {@code action} on it and prints the result, then the Core's state. */
    private static int withCore(CommandContext<CommandSourceStack> context, java.util.function.Function<CircleCoreBlockEntity, String> action)
            throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!(context.getSource().getLevel().getBlockEntity(pos) instanceof CircleCoreBlockEntity core)) {
            context.getSource().sendFailure(Component.literal("No Core at " + pos.toShortString()));
            return 0;
        }
        core.rescan();
        String result = action.apply(core);
        context.getSource().sendSuccess(() -> Component.literal(result), false);
        return showCircle(context);
    }

    /** Rescans the Core at {@code pos} now and prints what it sees. */
    private static int showCircle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        if (!(context.getSource().getLevel().getBlockEntity(pos) instanceof CircleCoreBlockEntity core)) {
            context.getSource().sendFailure(Component.literal("No Core at " + pos.toShortString()));
            return 0;
        }
        core.rescan();
        CircleScan scan = core.scan();
        String nodes = scan.nodes().stream()
                .map(node -> "r" + node.ring() + " " + node.side().inWorld(core.front()).getSerializedName() + " " + node.pattern())
                .collect(Collectors.joining(", "));
        String ignored = scan.ignoredModifiers().stream()
                .map(offset -> "(" + offset.dx() + ", " + offset.dz() + ")")
                .collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.literal("Core at " + pos.toShortString() + ": rank " + core.rank()
                + ", runes " + core.runes() + " (" + core.slots() + " slots), rings " + scan.rings() + "/" + core.maxRings()
                + ", nodes [" + nodes + "], ignored modifiers [" + ignored + "], children " + core.children()
                + core.seat().map(seat -> ", seat " + seat + " on " + core.frameRings() + " parent rings").orElse("") + ", instability "
                + core.instability() + " (threshold " + CircleCoreBlockEntity.settings().instabilityThreshold() + "), essentia " + core.essentia()
                + ", upkeep " + core.upkeep().map(Object::toString).orElse("undefined") + (core.lowRank() ? " (needs a higher rank)" : "")
                + ", running " + core.isRunning()), false);
        return scan.rings();
    }

    private static int showFlux(CommandContext<CommandSourceStack> context, FluxManager flux, ChunkPos chunk) {
        double amount = flux.get(context.getSource().getLevel(), chunk);
        String stage = flux.settings().stage(amount).name().toLowerCase(Locale.ROOT);
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Flux in chunk [%d, %d]: %.2f (%s)", chunk.x(), chunk.z(), amount, stage)), false);
        return (int) amount;
    }

    private static int writeReport(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            AspectReport.Summary summary = AspectReport.write(source.getServer().getServerDirectory().resolve("thaumory"));
            source.sendSuccess(() -> Component.literal("Wrote " + summary.file() + ": " + summary.withoutRecipe()
                    + " items without a recipe, " + summary.unresolved() + " unresolved, " + summary.gains() + " gaining recipes, " + summary.fell() + " fell"), false);
            return summary.withoutRecipe();
        } catch (IOException e) {
            source.sendFailure(Component.literal("Could not write aspect report: " + e.getMessage()));
            return 0;
        }
    }

    private static int showAspects(CommandContext<CommandSourceStack> context) {
        Item item = ItemArgument.getItem(context, "item").item().value();
        AspectList aspects = ItemAspects.get(item);
        String text = aspects.isEmpty() ? "none" : aspects.sortedByAmount().toString();
        String source = ItemAspects.source(item).name().toLowerCase(Locale.ROOT);
        context.getSource().sendSuccess(() -> Component.literal(item + ": " + text + " (" + source + ")"), false);
        return aspects.size();
    }
}
