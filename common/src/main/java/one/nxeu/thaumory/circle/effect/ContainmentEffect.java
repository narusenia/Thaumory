package one.nxeu.thaumory.circle.effect;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.circle.CrystalPool;
import one.nxeu.thaumory.item.FluxCrystalItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * Vinculum + Chaos, sustained (requirements §17.5). Draws Flux out of every chunk the range
 * touches and seals it into Flux crystals, dropped on the Core, one for each crystal's worth.
 */
final class ContainmentEffect implements CircleEffect {
    private static final String POOL = "pool";

    @Override
    public void apply(CircleContext context) {
        ServerLevel level = context.level();
        double rate = context.setting("flux_per_second", 1) * context.strength();
        AABB box = CircleRange.box(context);
        double drawn = 0;
        for (int cx = SectionPos.blockToSectionCoord(box.minX); cx <= SectionPos.blockToSectionCoord(box.maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(box.minZ); cz <= SectionPos.blockToSectionCoord(box.maxZ); cz++) {
                drawn += ThaumoryApi.flux().remove(level, new ChunkPos(cx, cz), rate);
            }
        }
        int perCrystal = Math.max(1, (int) Math.round(context.setting("flux_per_crystal", FluxCrystalItem.DEFAULT_FLUX)));
        CrystalPool.Step step = CrystalPool.add(context.data().getDoubleOr(POOL, 0), drawn, perCrystal);
        context.data().putDouble(POOL, step.held());
        if (step.crystals() <= 0) {
            return;
        }
        ItemStack crystals = new ItemStack(ThaumoryItems.FLUX_CRYSTAL.get(), step.crystals());
        if (perCrystal != FluxCrystalItem.DEFAULT_FLUX) {
            crystals.set(ThaumoryComponents.SEALED_FLUX.get(), perCrystal);
        }
        Vec3 at = Vec3.atCenterOf(context.core());
        level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, crystals, 0, 0, 0));
        context.affected(context.core());
    }

    /** What was drawn in but not yet sealed goes back to the Core's chunk. */
    @Override
    public void stop(CircleContext context) {
        double held = context.data().getDoubleOr(POOL, 0);
        if (held > 0) {
            ThaumoryApi.flux().add(context.level(), ChunkPos.containing(context.core()), held);
        }
    }
}
