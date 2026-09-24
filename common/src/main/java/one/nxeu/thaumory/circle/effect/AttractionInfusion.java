package one.nxeu.thaumory.circle.effect;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.aspect.data.ItemAspects;

/** Attraction on an item, passive: draws dropped items to the wearer, only those with slot 3's aspect if it has one. */
final class AttractionInfusion implements InfusionEffect {
    private static final double SPEED = 0.35;
    private static final double ARRIVED = 0.75;

    @Override
    public int period() {
        return 5;
    }

    @Override
    public boolean castable() {
        return true;
    }

    /** Everything in reach lands at the wearer's feet at once. */
    @Override
    public boolean cast(InfusionContext context) {
        Vec3 feet = context.wearer().position();
        var items = items(context, context.setting("scroll_radius", 8) + context.infusionLevel());
        items.forEach(item -> {
            item.teleportTo(feet.x, feet.y + 0.2, feet.z);
            item.setDeltaMovement(Vec3.ZERO);
        });
        return !items.isEmpty();
    }

    private static List<ItemEntity> items(InfusionContext context, double radius) {
        Optional<Aspect> filter = context.parameter();
        return context.level().getEntitiesOfClass(ItemEntity.class, context.wearer().getBoundingBox().inflate(radius),
                item -> filter.isEmpty() || ItemAspects.get(item.getItem()).contains(filter.get()));
    }

    @Override
    public void tick(InfusionContext context) {
        Vec3 target = context.wearer().position().add(0, 0.5, 0);
        for (ItemEntity item : items(context, context.setting("radius", 3) + context.infusionLevel())) {
            Vec3 towards = target.subtract(item.position());
            if (towards.length() > ARRIVED) {
                Vec3 motion = towards.normalize().scale(SPEED);
                item.setDeltaMovement(motion.x, Math.max(motion.y, 0.1), motion.z);
            }
        }
    }
}
