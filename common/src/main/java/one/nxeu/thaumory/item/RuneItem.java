package one.nxeu.thaumory.item;

import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.rune.RuneSettings;

/**
 * A rune of one aspect, the key a magic circle's Core takes. One item for every aspect, addons'
 * included: the aspect is a component. Made by pouring a jar into a blank rune ({@link JarItem}).
 */
public final class RuneItem extends Item {
    private static volatile RuneSettings settings = RuneSettings.DEFAULT;

    public RuneItem(Properties properties) {
        super(properties);
    }

    public static void updateSettings(RuneSettings newSettings) {
        settings = newSettings;
    }

    /** Essentia one rune takes. */
    public static int cost() {
        return settings.cost();
    }

    public static ItemStack of(Aspect aspect) {
        ItemStack stack = new ItemStack(ThaumoryItems.RUNE.get());
        stack.set(ThaumoryComponents.RUNE_ASPECT.get(), aspect.id());
        return stack;
    }

    /** Empty for a rune without an aspect or with one no longer registered. */
    public static Optional<Aspect> aspect(ItemStack stack) {
        return Optional.ofNullable(stack.get(ThaumoryComponents.RUNE_ASPECT.get())).flatMap(ThaumoryApi.aspects()::get);
    }
}
