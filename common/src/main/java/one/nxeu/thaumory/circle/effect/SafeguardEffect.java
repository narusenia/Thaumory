package one.nxeu.thaumory.circle.effect;

import dev.architectury.event.events.common.LifecycleEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Ordo + Terra, sustained. In range, explosions leave blocks standing and mobs act as if the
 * mobGriefing rule were off (requirements §17.4). The mixins on explosions, game rules and entity
 * ticking ask this class.
 */
public final class SafeguardEffect implements CircleEffect {
    /** Per circle, the centres of explosions it has spared blocks from since it last worked. */
    private static final ActiveAreas<Queue<BlockPos>> GUARDS = new ActiveAreas<>();
    /** The entity the server is ticking on this thread, whose mobGriefing checks we answer. */
    private static final ThreadLocal<Entity> TICKING = new ThreadLocal<>();

    SafeguardEffect() {}

    static void registerEvents() {
        LifecycleEvent.SERVER_STOPPED.register(server -> GUARDS.clear());
    }

    @Override
    public void apply(CircleContext context) {
        Queue<BlockPos> spared = GUARDS.renew(context, CircleRange.box(context), old -> old == null ? new ConcurrentLinkedQueue<>() : old);
        for (BlockPos pos; (pos = spared.poll()) != null; ) {
            context.affected(pos);
        }
    }

    @Override
    public void stop(CircleContext context) {
        GUARDS.remove(context);
    }

    /** {@code blocks} an explosion at {@code centre} is about to break, less those a safeguard covers. */
    public static List<BlockPos> spare(ServerLevel level, Vec3 centre, List<BlockPos> blocks) {
        if (GUARDS.isEmpty(level)) {
            return blocks;
        }
        List<BlockPos> kept = new ArrayList<>(blocks.size());
        Set<Queue<BlockPos>> guarded = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : blocks) {
            Optional<Queue<BlockPos>> guard = GUARDS.find(level, Vec3.atCenterOf(pos));
            if (guard.isEmpty()) {
                kept.add(pos);
            } else if (!level.getBlockState(pos).isAir()) {
                guarded.add(guard.get());
            }
        }
        BlockPos at = BlockPos.containing(centre);
        guarded.forEach(spared -> spared.add(at));
        return kept;
    }

    /** Called as the server starts ticking {@code entity}; hand the result back to {@link #exitTick}. */
    public static Entity enterTick(Entity entity) {
        Entity previous = TICKING.get();
        TICKING.set(entity);
        return previous;
    }

    public static void exitTick(Entity previous) {
        if (previous == null) {
            TICKING.remove();
        } else {
            TICKING.set(previous);
        }
    }

    /** Whether the entity ticking now stands in a safeguard, so mobGriefing reads false for it. */
    public static boolean stopsGriefing() {
        Entity entity = TICKING.get();
        return entity != null && !(entity instanceof Player) && entity.level() instanceof ServerLevel level
                && GUARDS.find(level, entity.position()).isPresent();
    }
}
