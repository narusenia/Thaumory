package one.nxeu.thaumory.api.infusion;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.Experimental;
import one.nxeu.thaumory.api.aspect.Aspect;

/** Who an infused effect works for this time, and how strongly. */
@Experimental
public interface InfusionContext {
    ServerLevel level();

    /** Who has the item equipped, or is using it. */
    LivingEntity wearer();

    /** The item the effect is burnt into. */
    ItemStack stack();

    /** The infusion's level, from 1. */
    int infusionLevel();

    /** The slot 3 aspect when it was infused, if any. What it means is up to the effect. */
    Optional<Aspect> parameter();

    /**
     * A number from the combination's {@code item_settings} in the datapack, or {@code fallback} when
     * the file does not set it.
     */
    double setting(String key, double fallback);
}
