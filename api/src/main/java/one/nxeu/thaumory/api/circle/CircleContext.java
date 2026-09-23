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
}
