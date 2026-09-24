package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.flux.pollution.PollutedBlock;
import one.nxeu.thaumory.flux.pollution.PollutionIndex;
import one.nxeu.thaumory.flux.pollution.PollutionRules;

/**
 * Ordo + Lux, sustained. Takes Flux out of every chunk the range touches and turns polluted blocks
 * in range back. With a Primal in slot 3 the Flux taken is gathered back as that aspect's Essentia
 * in the Core.
 */
final class PurificationEffect implements CircleEffect {
    private static final String POOL = "pool";

    /** Turns up to {@code limit} polluted blocks in {@code box} back into what they were. */
    private static void restore(CircleContext context, ServerLevel level, AABB box, int limit) {
        PollutionIndex index = PollutionIndex.of(level);
        int restored = 0;
        for (BlockPos pos : index.within(box)) {
            if (restored >= limit) {
                return;
            }
            Block block = level.getBlockState(pos).getBlock();
            Optional<Block> original = block instanceof PollutedBlock ? PollutionRules.restoreFor(block) : Optional.empty();
            if (original.isEmpty()) {
                // Gone already, or no rule says what it was.
                if (!(block instanceof PollutedBlock)) {
                    index.remove(pos);
                }
                continue;
            }
            level.setBlockAndUpdate(pos, original.get().defaultBlockState());
            context.affected(pos);
            restored++;
        }
    }

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
        restore(context, level, box, (int) Math.ceil(context.setting("restore_per_second", 2) * context.strength()));
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
