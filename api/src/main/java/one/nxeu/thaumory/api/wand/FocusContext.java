package one.nxeu.thaumory.api.wand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import one.nxeu.thaumory.api.Experimental;

/** Who casts a focus's spell this time, and with what wand. */
@Experimental
public interface FocusContext {
    ServerLevel level();

    Player caster();

    /** The wand the spell is cast from. */
    ItemStack wand();

    /** How strongly the wand's core casts: 1 for a wooden core. What it scales is up to the spell. */
    double power();

    /** The blocks the wand's core cannot break, for spells that dig. */
    TagKey<Block> incorrectFor();

    /**
     * A number from the focus's {@code settings} in the datapack, or {@code fallback} when the file
     * does not set it.
     */
    double setting(String key, double fallback);

    /**
     * Takes one cast's cost, as the focus's file sets it, from the Essentia in the wand.
     *
     * @return false, taking nothing, if the wand holds too little of some aspect
     */
    boolean pay();
}
