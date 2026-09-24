package one.nxeu.thaumory.circle.effect;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;

/**
 * Lightness, breath and night sight worn: the circle's status effects kept on the wearer alone,
 * renewed every second so they wear off soon after taking the item off. Lightness also takes away
 * the wearer's fall damage (requirements §10.2).
 */
final class AuraInfusion implements InfusionEffect {
    /** A cushion not renewed for this long has been taken off (the wearer logged out, say). */
    private static final long STALE = 40;

    /** Per wearer with a lightness item on, the game time it was last renewed. */
    private static final Map<UUID, Long> CUSHIONED = new ConcurrentHashMap<>();

    private final List<AuraEffect.Aura> auras;
    private final boolean cushionsFalls;

    AuraInfusion(boolean cushionsFalls, AuraEffect.Aura... auras) {
        this.auras = List.of(auras);
        this.cushionsFalls = cushionsFalls;
    }

    static void registerEvents() {
        EntityEvent.LIVING_HURT.register((entity, source, amount) -> {
            if (!source.is(DamageTypeTags.IS_FALL) || !(entity.level() instanceof ServerLevel level)) {
                return EventResult.pass();
            }
            Long renewed = CUSHIONED.get(entity.getUUID());
            return renewed != null && level.getGameTime() - renewed <= STALE ? EventResult.interruptFalse() : EventResult.pass();
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> CUSHIONED.clear());
    }

    @Override
    public void tick(InfusionContext context) {
        if (cushionsFalls) {
            CUSHIONED.put(context.wearer().getUUID(), context.level().getGameTime());
        }
        int steps = context.infusionLevel() - 1;
        for (AuraEffect.Aura aura : auras) {
            int amplifier = aura.amplifier() + (aura.leveled() ? steps : 0);
            context.wearer().addEffect(new MobEffectInstance(aura.effect(), aura.duration(), amplifier, true, false, true));
        }
    }

    @Override
    public void stop(InfusionContext context) {
        if (cushionsFalls) {
            CUSHIONED.remove(context.wearer().getUUID());
        }
    }
}
