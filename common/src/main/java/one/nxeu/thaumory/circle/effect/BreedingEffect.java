package one.nxeu.thaumory.circle.effect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.circle.BreedingPairs;

/**
 * Bestia + Vita, sustained. Every so often puts a pair of grown animals of one kind in love, and
 * vanilla breeding does the rest; a kind with enough of its own in range is left alone
 * (requirements §17.4).
 */
final class BreedingEffect implements CircleEffect {
    private static final String SECONDS = "seconds";

    @Override
    public void apply(CircleContext context) {
        int every = Math.max(1, (int) Math.round(context.setting("interval_seconds", 10)));
        int second = context.data().getIntOr(SECONDS, 0);
        context.data().putInt(SECONDS, (second + 1) % every);
        if (second != 0) {
            return;
        }
        Map<EntityType<?>, List<Animal>> ready = new HashMap<>();
        Map<EntityType<?>, Integer> population = new HashMap<>();
        for (Animal animal : context.level().getEntitiesOfClass(Animal.class, CircleRange.box(context), Animal::isAlive)) {
            population.merge(animal.getType(), 1, Integer::sum);
            if (animal.getAge() == 0 && animal.canFallInLove()) {
                ready.computeIfAbsent(animal.getType(), type -> new ArrayList<>()).add(animal);
            }
        }
        int cap = (int) Math.round(context.setting("max_per_kind", 16));
        int pairs = (int) Math.ceil(context.strength());
        for (List<Animal> pair : BreedingPairs.choose(ready, population, cap, pairs, context.level().getRandom()::nextInt)) {
            for (Animal animal : pair) {
                animal.setInLove(null);
                context.affected(animal);
            }
        }
    }
}
