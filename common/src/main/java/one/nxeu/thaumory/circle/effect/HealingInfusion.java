package one.nxeu.thaumory.circle.effect;

import net.minecraft.world.entity.animal.Animal;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/** Healing on an item, passive: heals the wearer now and then, or the animals nearby with Bestia. */
final class HealingInfusion implements InfusionEffect {
    @Override
    public int period() {
        return 100;
    }

    @Override
    public void tick(InfusionContext context) {
        float amount = (float) (context.setting("amount_per_level", 1) * context.infusionLevel());
        if (context.parameter().filter(ThaumoryAspects.BESTIA::equals).isPresent()) {
            double radius = context.setting("animal_radius", 4);
            context.level().getEntitiesOfClass(Animal.class, context.wearer().getBoundingBox().inflate(radius), Animal::isAlive)
                    .forEach(animal -> animal.heal(amount));
        } else {
            context.wearer().heal(amount);
        }
    }

    @Override
    public boolean castable() {
        return true;
    }

    /** At once, a good deal more than a tick of the passive. */
    @Override
    public boolean cast(InfusionContext context) {
        float amount = (float) (context.setting("scroll_amount_per_level", 4) * context.infusionLevel());
        if (context.parameter().filter(ThaumoryAspects.BESTIA::equals).isPresent()) {
            double radius = context.setting("animal_radius", 4);
            var animals = context.level().getEntitiesOfClass(Animal.class, context.wearer().getBoundingBox().inflate(radius), Animal::isAlive);
            animals.forEach(animal -> animal.heal(amount));
            return !animals.isEmpty();
        }
        context.wearer().heal(amount);
        return true;
    }
}
