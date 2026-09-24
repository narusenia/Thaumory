package one.nxeu.thaumory.circle.effect;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
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
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/**
 * A sustained circle that keeps status effects on what it targets in range: the life circles
 * (lightness, breath, night sight) and the binding and withering defence circles. The effects are
 * renewed every second and kept short, so they wear off soon after leaving. Lightness also takes
 * away fall damage on landing in range (requirements §17.4).
 */
final class AuraEffect implements CircleEffect {
    /**
     * One status effect the circle keeps up, at amplifier {@code amplifier}. {@code leveled} ones go
     * up one level per step of strength past 1.
     */
    record Aura(Holder<MobEffect> effect, int duration, int amplifier, boolean leveled) {}

    private record Cushion(Predicate<Entity> target, Queue<BlockPos> landed) {}

    private static final ActiveAreas<Cushion> CUSHIONS = new ActiveAreas<>();

    private final Function<Optional<Aspect>, Predicate<Entity>> targets;
    private final List<Aura> auras;
    private final boolean cushionsFalls;

    /** {@code targets} turns the slot 3 rune into what the circle works on. */
    AuraEffect(Function<Optional<Aspect>, Predicate<Entity>> targets, boolean cushionsFalls, Aura... auras) {
        this.targets = targets;
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
        Predicate<Entity> target = targets.apply(context.parameter());
        AABB box = CircleRange.box(context);
        if (cushionsFalls) {
            Queue<BlockPos> landed = CUSHIONS.renew(context, box,
                    old -> new Cushion(target, old == null ? new ConcurrentLinkedQueue<>() : old.landed())).landed();
            for (BlockPos pos; (pos = landed.poll()) != null; ) {
                context.affected(pos);
            }
        }
        int steps = Math.max(0, (int) Math.ceil(context.strength()) - 1);
        for (LivingEntity entity : context.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && target.test(e))) {
            boolean fresh = false;
            for (Aura aura : auras) {
                fresh |= !entity.hasEffect(aura.effect());
                int amplifier = aura.amplifier() + (aura.leveled() ? steps : 0);
                entity.addEffect(new MobEffectInstance(aura.effect(), aura.duration(), amplifier, true, false, true));
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

    /** The life circles: players when slot 3 is empty, else what the rune picks as for the ward. */
    static Predicate<Entity> playersUnlessPicked(Optional<Aspect> parameter) {
        return parameter.map(WardEffect::target).orElse(entity -> entity instanceof Player player && !player.isSpectator());
    }

    /** The defence circles: hostile mobs when slot 3 is empty, else what the rune picks as for the ward. */
    static Predicate<Entity> enemiesUnlessPicked(Optional<Aspect> parameter) {
        return WardEffect.target(parameter.orElse(ThaumoryAspects.CHAOS));
    }
}
