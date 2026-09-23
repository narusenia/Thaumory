package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Ordo + Lux, sustained. Takes Flux out of every chunk the range touches. With a Primal in slot 3
 * the Flux taken is gathered back as that aspect's Essentia in the Core.
 */
final class PurificationEffect implements CircleEffect {
    private static final String POOL = "pool";

    @Override
    public void apply(CircleContext context) {
        ServerLevel level = context.level();
        double rate = context.setting("flux_per_second", 0.5) * context.strength();
        AABB box = CircleRange.box(context);
        double removed = 0;
        for (int cx = SectionPos.blockToSectionCoord(box.minX); cx <= SectionPos.blockToSectionCoord(box.maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(box.minZ); cz <= SectionPos.blockToSectionCoord(box.maxZ); cz++) {
                removed += ThaumoryApi.flux().remove(level, new ChunkPos(cx, cz), rate);
            }
        }
        Optional<Aspect> gather = context.parameter().filter(Aspect::isPrimal);
        if (gather.isEmpty() || removed <= 0) {
            return;
        }
        double perEssentia = Math.max(0.01, context.setting("flux_per_essentia", 2));
        double pool = context.data().getDoubleOr(POOL, 0) + removed;
        int essentia = (int) Math.floor(pool / perEssentia);
        if (essentia > 0) {
            context.store(gather.get(), essentia);
            // What does not fit in a full Core is lost with the Flux it came from.
            pool -= essentia * perEssentia;
        }
        context.data().putDouble(POOL, pool);
    }
}
