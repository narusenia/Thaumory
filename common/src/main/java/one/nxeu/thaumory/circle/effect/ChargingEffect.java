package one.nxeu.thaumory.circle.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.infusion.ItemEssentia;
import one.nxeu.thaumory.item.ThaumoryComponents;

/**
 * Arcanum + Vinculum, sustained. Moves Essentia from the Core into the item on its pedestal, as much
 * of each aspect the item stores for its active effect as the rate allows. It has no infusion effect,
 * so it cannot be burnt into anything itself.
 */
final class ChargingEffect implements CircleEffect {
    @Override
    public void apply(CircleContext context) {
        if (!(context.level().getBlockEntity(context.core()) instanceof CircleCoreBlockEntity core)) {
            return;
        }
        ItemStack item = core.pedestalItem();
        Infusions infusions = item.getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY);
        if (item.isEmpty() || infusions.storedAspects().isEmpty()) {
            return;
        }
        int rate = (int) Math.ceil(context.setting("per_second", 4) * context.strength());
        AspectList.Builder offered = AspectList.builder();
        for (AspectStack stack : core.essentia().stacks()) {
            offered.add(stack.aspect(), Math.min(rate, stack.amount()));
        }
        AspectList stored = item.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
        ItemEssentia.Fill fill = ItemEssentia.fill(stored, offered.build(), infusions.storedAspects(),
                CircleCoreBlockEntity.settings().infusion().itemEssentia());
        if (fill.taken().isEmpty()) {
            return;
        }
        core.setEssentia(core.essentia().minus(fill.taken()));
        ItemStack charged = item.copy();
        InfusionRuntime.setStored(charged, fill.stored());
        core.setPedestalItem(charged);
        var pos = context.core();
        context.level().sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.4);
    }
}
