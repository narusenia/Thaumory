package one.nxeu.thaumory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.Locale;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.data.ItemAspects;

/** Debug commands under {@code /thaumory}. Operators only. */
public final class ThaumoryCommands {
    private ThaumoryCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("thaumory")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("aspects")
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(ThaumoryCommands::showAspects))));
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
