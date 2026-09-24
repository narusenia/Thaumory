package one.nxeu.thaumory.circle.effect;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Lightness (Aer + Terra), breath (Aqua + Aer) and night sight (Lux + Umbra), sustained. Keeps
 * status effects on what slot 3 picks in range: players when it is empty, animals with Bestia,
 * hostile mobs with Chaos. The effects are renewed every second and kept short, so they wear off
 * soon after leaving. Lightness also takes away fall damage on landing in range (requirements §17.4).
 */
final class AuraEffect implements CircleEffect {
    /** One status effect the circle keeps up. {@code leveled} ones take their level from the strength. */
    record Aura(Holder<MobEffect> effect, int duration, boolean leveled) {}

    private record Cushion(Predicate<Entity> target, Queue<BlockPos> landed) {}

    private static final ActiveAreas<Cushion> CUSHIONS = new ActiveAreas<>();

    private final List<Aura> auras;
    private final boolean cushionsFalls;

    AuraEffect(boolean cushionsFalls, Aura... auras) {
        this.auras = List.of(auras);
        this.cushionsFalls = cushionsFalls;
    }

    static void registerEvents() {
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (!source.is(DamageTypeTags.IS_FALL) || !(entity.level() instanceof ServerLevel level)) {
                return EventResult.pass();
            }
            Optional<Cushion> cushion = CUSHIONS.find(level, entity.position(), c -> c.target().test(entity));
            if (cushion.isEmpty()) {
                return EventResult.pass();
            }
            // Marked when the circle next works, where the context is at hand.
            cushion.get().landed().add(entity.blockPosition());
            return EventResult.interruptFalse();
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> CUSHIONS.clear());
    }

    @Override
    public void apply(CircleContext context) {
        Predicate<Entity> target = target(context.parameter());
        AABB box = CircleRange.box(context);
        if (cushionsFalls) {
            Queue<BlockPos> landed = CUSHIONS.renew(context, box,
                    old -> new Cushion(target, old == null ? new ConcurrentLinkedQueue<>() : old.landed())).landed();
            for (BlockPos pos; (pos = landed.poll()) != null; ) {
                context.affected(pos);
            }
        }
        int amplifier = Math.max(0, (int) Math.ceil(context.strength()) - 1);
        for (LivingEntity entity : context.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && target.test(e))) {
            boolean fresh = false;
            for (Aura aura : auras) {
                fresh |= !entity.hasEffect(aura.effect());
                entity.addEffect(new MobEffectInstance(aura.effect(), aura.duration(), aura.leveled() ? amplifier : 0, true, false, true));
            }
            if (fresh) {
                context.affected(entity);
            }
        }
    }

    @Override
    public void stop(CircleContext context) {
        if (cushionsFalls) {
            CUSHIONS.remove(context);
        }
    }

    static Predicate<Entity> target(Optional<Aspect> parameter) {
        return parameter.map(WardEffect::target).orElse(entity -> entity instanceof Player player && !player.isSpectator());
    }
}
