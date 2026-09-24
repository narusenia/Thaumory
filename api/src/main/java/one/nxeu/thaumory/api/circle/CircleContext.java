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

    /** The Core at the circle's centre. */
    BlockPos core();

    /** The slot 3 rune's aspect, if any. What it means is up to the effect. */
    Optional<Aspect> parameter();

    /** 1 for a plain circle; amplifying modifiers raise it, economizing ones lower it. */
    double strength();

    /** How far the effect reaches from the Core, in blocks, after the extending modifiers. */
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
     * Marks a block the effect has just worked on: motes in the colours of the circle's two runes
     * rise from it. Call it for what the effect changed, not for everything it looked at.
     */
    void showAffected(BlockPos pos);

    /** Marks a living thing or dropped item the effect has just worked on, as {@link #showAffected(BlockPos)}. */
    void showAffected(Entity entity);
}
