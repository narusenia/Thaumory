package one.nxeu.thaumory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.util.Locale;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.flux.FluxManager;

/** Debug commands under {@code /thaumory}. Operators only. */
public final class ThaumoryCommands {
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
                        })))));
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
