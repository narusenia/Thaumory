package one.nxeu.thaumory.circle.effect;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/**
 * Vinculum + Ordo, sustained. Keeps one kind of creature out: Bestia animals, Mors the undead,
 * Chaos anything hostile. They do not spawn naturally in range, and those that walk in are pushed
 * back out.
 */
final class WardEffect implements CircleEffect {
    private static final int PERIOD = 5;
    /** A ward that has not worked for this long is taken to have stopped (its chunk unloaded, say). */
    private static final long STALE = 40;
    private static final double PUSH = 0.4;

    private record Ward(AABB box, Predicate<Entity> target, long lastSeen) {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, Ward>> ACTIVE = new ConcurrentHashMap<>();

    static void registerEvents() {
        EntityEvent.LIVING_CHECK_SPAWN.register((entity, world, x, y, z, reason, spawner) -> {
            if ((reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION)
                    || !(world instanceof ServerLevel level)) {
                return EventResult.pass();
            }
            Map<BlockPos, Ward> wards = ACTIVE.getOrDefault(level.dimension(), Map.of());
            long now = level.getGameTime();
            for (Ward ward : wards.values()) {
                if (now - ward.lastSeen() <= STALE && ward.box().contains(x, y, z) && ward.target().test(entity)) {
                    return EventResult.interruptFalse();
                }
            }
            return EventResult.pass();
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> ACTIVE.clear());
    }

    @Override
    public int period() {
        return PERIOD;
    }

    @Override
    public void apply(CircleContext context) {
        Optional<Predicate<Entity>> target = context.parameter().map(WardEffect::target);
        if (target.isEmpty()) {
            return;
        }
        ServerLevel level = context.level();
        AABB box = CircleRange.box(context);
        ACTIVE.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                .put(context.core().immutable(), new Ward(box, target.get(), level.getGameTime()));

        Vec3 centre = CircleRange.centre(context);
        double push = PUSH * context.strength();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> !(e instanceof Player) && target.get().test(e))) {
            Vec3 away = entity.position().subtract(centre).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) {
                away = new Vec3(1, 0, 0);
            }
            away = away.normalize().scale(push);
            entity.push(away.x, 0.1, away.z);
        }
    }

    @Override
    public void stop(CircleContext context) {
        Map<BlockPos, Ward> wards = ACTIVE.get(context.level().dimension());
        if (wards != null) {
            wards.remove(context.core());
        }
    }

    private static Predicate<Entity> target(Aspect aspect) {
        if (aspect.equals(ThaumoryAspects.BESTIA)) {
            return entity -> entity instanceof Animal;
        }
        if (aspect.equals(ThaumoryAspects.MORS)) {
            return entity -> entity.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
        }
        if (aspect.equals(ThaumoryAspects.CHAOS)) {
            return entity -> entity instanceof Enemy;
        }
        return entity -> false;
    }
}
