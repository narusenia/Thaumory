package one.nxeu.thaumory.circle.effect;

import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;

/**
 * Binding and withering on a main-hand item: what it hits gets the circle's status effects for
 * {@code seconds} + level seconds (requirements §10.2).
 */
final class AfflictionInfusion implements InfusionEffect {
    private final double seconds;
    private final List<AuraEffect.Aura> auras;

    /** The auras' own durations are not used; each hit lasts {@code seconds} + level. */
    AfflictionInfusion(double seconds, AuraEffect.Aura... auras) {
        this.seconds = seconds;
        this.auras = List.of(auras);
    }

    @Override
    public void attack(InfusionContext context, LivingEntity target) {
        int ticks = (int) Math.round((context.setting("seconds", seconds) + context.infusionLevel()) * 20);
        int steps = context.infusionLevel() - 1;
        for (AuraEffect.Aura aura : auras) {
            int amplifier = aura.amplifier() + (aura.leveled() ? steps : 0);
            target.addEffect(new MobEffectInstance(aura.effect(), ticks, amplifier), context.wearer());
        }
    }
}
