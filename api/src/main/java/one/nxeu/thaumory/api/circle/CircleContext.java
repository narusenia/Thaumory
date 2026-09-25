package one.nxeu.thaumory.api.circle;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import one.nxeu.thaumory.api.aspect.Aspect;

/** Where and how strongly a circle's effect works this time. */
public interface CircleContext {
    ServerLevel level();

    /**
     * The Core running the effect: it holds the Essentia, and what the effect puts out comes up
     * there. For a sub-circle this is a Core on a node of its parent's circle, not the centre.
     */
    BlockPos core();

    /**
     * The centre of the effect's range: the Core itself, or for a sub-circle the parent's Core.
     * Measure the range from here.
     */
    default BlockPos centre() {
        return core();
    }

    /** The slot 3 rune's aspect, if any. What it means is up to the effect. */
    Optional<Aspect> parameter();

    /** 1 for a plain circle; amplifying modifiers raise it, economizing ones lower it. */
    double strength();

    /** How far the effect reaches from {@link #centre()}, in blocks, after the extending modifiers. */
    double radius();

    /** Who activated a triggered circle, if anyone did. Always empty for sustained circles. */
    Optional<Entity> activator();

    /**
     * State kept with the Core for this circle while it runs, saved with the world. Sustained
     * effects use it to remember what they did (the lights they placed, say) so {@link
     * CircleEffect#stop} can undo it.
     */
    CompoundTag data();

    /**
     * A number from the combination's {@code settings} in the datapack, or {@code fallback} when
     * the file does not set it.
     */
    double setting(String key, double fallback);

    /**
     * Puts Essentia into the Core, as far as its runes let in and its capacity allows.
     *
     * @return how much went in
     */
    int store(Aspect aspect, int amount);

    /**
     * Records a block the effect has just worked on: motes in the colours of the circle's two runes
     * rise from it, and the circle gives off the Flux its combination's {@code work_flux} setting
     * asks for each piece of work. Call it for what the effect changed, not for everything it
     * looked at.
     */
    void affected(BlockPos pos);

    /** Records a living thing or dropped item the effect has just worked on, as {@link #affected(BlockPos)}. */
    void affected(Entity entity);

    /**
     * Pays for work beyond the activation or upkeep: {@code amount}, scaled by the circle's cost
     * modifiers and rounded (at least 1), of each of the two effect runes' Essentia from the Core.
     *
     * @return false, taking nothing, if the Core holds too little
     */
    boolean pay(int amount);
}
