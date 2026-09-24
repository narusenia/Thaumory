package one.nxeu.thaumory.circle.effect;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.circle.CircleContext;

/**
 * The ranges of running circles of one kind, so that something happening elsewhere (a fall, an
 * explosion) can ask whether it falls inside one. Each circle renews its area every time it works.
 */
final class ActiveAreas<T> {
    /** An area that has not been renewed for this long is taken to have stopped (its chunk unloaded, say). */
    private static final long STALE = 40;

    private record Area<T>(AABB box, T value, long lastSeen) {}

    private final Map<ResourceKey<Level>, Map<BlockPos, Area<T>>> areas = new ConcurrentHashMap<>();

    /**
     * Renews the circle's area. {@code value} gets what the area held before, or null the first time.
     *
     * @return the value now held
     */
    T renew(CircleContext context, AABB box, UnaryOperator<T> value) {
        Map<BlockPos, Area<T>> inLevel = areas.computeIfAbsent(context.level().dimension(), k -> new ConcurrentHashMap<>());
        Area<T> old = inLevel.get(context.core());
        T next = value.apply(old == null ? null : old.value());
        inLevel.put(context.core().immutable(), new Area<>(box, next, context.level().getGameTime()));
        return next;
    }

    void remove(CircleContext context) {
        Map<BlockPos, Area<T>> inLevel = areas.get(context.level().dimension());
        if (inLevel != null) {
            inLevel.remove(context.core());
        }
    }

    /** The value of a live area containing {@code pos} that passes {@code test}, if any. */
    Optional<T> find(Level level, Vec3 pos, Predicate<T> test) {
        Map<BlockPos, Area<T>> inLevel = areas.get(level.dimension());
        if (inLevel == null || inLevel.isEmpty()) {
            return Optional.empty();
        }
        long now = level.getGameTime();
        for (Area<T> area : inLevel.values()) {
            if (now - area.lastSeen() <= STALE && area.box().contains(pos) && test.test(area.value())) {
                return Optional.of(area.value());
            }
        }
        return Optional.empty();
    }

    Optional<T> find(Level level, Vec3 pos) {
        return find(level, pos, value -> true);
    }

    boolean isEmpty(Level level) {
        Map<BlockPos, Area<T>> inLevel = areas.get(level.dimension());
        return inLevel == null || inLevel.isEmpty();
    }

    void clear() {
        areas.clear();
    }
}
