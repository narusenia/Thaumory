package one.nxeu.thaumory.api.infusion;

import net.minecraft.world.entity.LivingEntity;
import one.nxeu.thaumory.api.Experimental;

/**
 * What a circle's effect does once burnt into an item, registered in
 * {@link one.nxeu.thaumory.api.ThaumoryApi#infusionEffects()} under the same id as its
 * {@link one.nxeu.thaumory.api.circle.CircleEffect}. A circle effect without one cannot be infused.
 *
 * <p>A passive effect works while the item is equipped (held in either hand or worn) and costs
 * nothing. An active effect is used with a key and pays from the Essentia stored in the item; what
 * it pays comes from the datapack. On a scroll, either kind works once, for free ({@link #cast}).
 */
@Experimental
public interface InfusionEffect {
    /** Whether the effect is used with the key rather than working while equipped. */
    default boolean active() {
        return false;
    }

    /** Ticks between calls to {@link #tick} while a passive effect is equipped. */
    default int period() {
        return 20;
    }

    /**
     * A passive effect: called every {@link #period()} ticks while equipped. Several equipped items with
     * the same effect call it once, with the highest level among them.
     */
    default void tick(InfusionContext context) {}

    /**
     * A passive effect: called once when it stops working for a wearer (the item was taken off, say, or
     * the wearer left). Undo what should not outlive it here.
     */
    default void stop(InfusionContext context) {}

    /** A passive effect on the main-hand item: its wearer has just hit {@code target} with it. */
    default void attack(InfusionContext context, LivingEntity target) {}

    /**
     * An active effect: checked before paying. Return false when there is nothing to act on (nowhere to
     * teleport to, say) and the use is called off at no cost.
     */
    default boolean canUse(InfusionContext context) {
        return true;
    }

    /** An active effect: called once per use, after paying. */
    default void use(InfusionContext context) {}

    /** Whether the effect can be burnt into a scroll. By default only active effects can. */
    default boolean castable() {
        return active();
    }

    /**
     * A scroll with this effect was used up, at no Essentia; the context's item is the scroll. Return
     * false when nothing came of it, and the scroll is kept. By default an active effect is used.
     */
    default boolean cast(InfusionContext context) {
        if (active() && canUse(context)) {
            use(context);
            return true;
        }
        return false;
    }
}
