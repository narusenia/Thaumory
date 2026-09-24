package one.nxeu.thaumory.circle.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.circle.SortingTargets;

/**
 * Ordo + Tempestas, sustained. Puts dropped items in range away into containers in range, a few
 * stacks a second: into one already holding the same item if there is one, otherwise the nearest
 * with room. With an aspect in slot 3, only items that contain it (requirements §17.4).
 */
final class SortingEffect implements CircleEffect {
    @Override
    public void apply(CircleContext context) {
        Optional<Aspect> filter = context.parameter();
        List<ItemEntity> items = CircleRange.items(context,
                item -> filter.isEmpty() || ItemAspects.get(item.getItem()).contains(filter.get()));
        if (items.isEmpty()) {
            return;
        }
        List<Placed> containers = containers(context);
        if (containers.isEmpty()) {
            return;
        }
        int left = CircleRange.itemsPerCall(context);
        for (ItemEntity item : items) {
            if (left-- <= 0) {
                return;
            }
            ItemStack stack = item.getItem().copy();
            int before = stack.getCount();
            for (Container target : order(containers, stack)) {
                stack = HopperBlockEntity.addItem(null, target, stack, Direction.UP);
                if (stack.isEmpty()) {
                    break;
                }
            }
            if (stack.getCount() == before) {
                continue;
            }
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            context.affected(item);
        }
    }

    private record Placed(BlockPos pos, Container container) {}

    private static List<Container> order(List<Placed> containers, ItemStack stack) {
        List<SortingTargets.Candidate<Container>> candidates = new ArrayList<>();
        for (Placed placed : containers) {
            boolean same = placed.container().hasAnyMatching(held -> ItemStack.isSameItemSameComponents(held, stack));
            candidates.add(new SortingTargets.Candidate<>(placed.container(), same, placed.pos().distToCenterSqr(Vec3.ZERO)));
        }
        return SortingTargets.order(candidates);
    }

    /** Every container whose block is in range, by its offset from the Core. */
    private static List<Placed> containers(CircleContext context) {
        ServerLevel level = context.level();
        AABB box = CircleRange.box(context);
        List<Placed> found = new ArrayList<>();
        for (int cx = SectionPos.blockToSectionCoord(box.minX); cx <= SectionPos.blockToSectionCoord(box.maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(box.minZ); cz <= SectionPos.blockToSectionCoord(box.maxZ); cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockPos pos : level.getChunk(cx, cz).getBlockEntities().keySet()) {
                    if (!box.contains(Vec3.atCenterOf(pos)) || pos.equals(context.core())) {
                        continue;
                    }
                    Container container = HopperBlockEntity.getContainerAt(level, pos);
                    if (container != null) {
                        found.add(new Placed(pos.subtract(context.core()), container));
                    }
                }
            }
        }
        return found;
    }
}
