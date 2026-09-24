package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ChunkPos;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.circle.CircleMelt;
import one.nxeu.thaumory.crucible.CrucibleSettings;
import one.nxeu.thaumory.item.ThaumoryComponents;

/**
 * Ignis + Chaos, sustained. Melts dropped items in range into Essentia, one at a time and a few a
 * second, at the Crucible's rates. The aspect in slot 3 goes into the Core; everything else, and
 * whatever the Core has no room for, becomes Flux (requirements §17.4).
 */
final class MeltingEffect implements CircleEffect {
    @Override
    public void apply(CircleContext context) {
        ServerLevel level = context.level();
        Optional<Aspect> kept = context.parameter();
        CrucibleSettings settings = CrucibleBlockEntity.settings();
        int left = CircleRange.itemsPerCall(context);
        int flux = 0;
        for (ItemEntity item : CircleRange.items(context, MeltingEffect::meltable)) {
            ItemStack stack = item.getItem();
            AspectList aspects = ItemAspects.get(stack);
            double ratio = ItemAspects.source(stack.getItem()) == ItemAspects.Source.DATAPACK
                    ? settings.meltRatio().manual()
                    : settings.meltRatio().estimated();
            while (left > 0 && !stack.isEmpty()) {
                left--;
                CircleMelt.Result result = CircleMelt.melt(aspects, ratio, kept, Integer.MAX_VALUE);
                int stored = kept.map(aspect -> context.store(aspect, result.stored())).orElse(0);
                flux += result.flux() + result.stored() - stored;
                ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
                stack.shrink(1);
                if (remainder != null) {
                    level.addFreshEntity(new ItemEntity(level, item.getX(), item.getY(), item.getZ(), remainder.create()));
                }
            }
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            context.affected(item);
            if (left <= 0) {
                break;
            }
        }
        if (flux > 0) {
            ThaumoryApi.flux().add(level, ChunkPos.containing(context.core()), flux);
        }
    }

    /** Anything with aspects, bar a jar with Essentia in it, which the player should empty first. */
    private static boolean meltable(ItemEntity item) {
        ItemStack stack = item.getItem();
        return !ItemAspects.get(stack).isEmpty() && !stack.has(ThaumoryComponents.JAR_CONTENTS.get());
    }
}
