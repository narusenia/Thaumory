package one.nxeu.thaumory.crucible;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectCancellation;

/**
 * What a Crucible holds: mixed Essentia, the water level (0 to {@link #MAX_WATER}), and how much
 * Essentia has melted since the water last dropped a level. Immutable.
 */
public record CrucibleTank(AspectList contents, int water, int essentiaSinceWaterDrop) {
    public static final int MAX_WATER = 3;
    public static final CrucibleTank EMPTY = new CrucibleTank(AspectList.empty(), 0, 0);

    /**
     * @param tank     after melting
     * @param overflow Essentia that did not fit and becomes Flux
     */
    public record MeltResult(CrucibleTank tank, int overflow) {}

    public CrucibleTank {
        water = Math.clamp(water, 0, MAX_WATER);
    }

    public boolean hasWater() {
        return water > 0;
    }

    public CrucibleTank withWater(int newWater) {
        return new CrucibleTank(contents, newWater, essentiaSinceWaterDrop);
    }

    /**
     * Melts one item with the given aspects, keeping {@code ratio} of each (rounded down). Aspects
     * go in from the largest; what does not fit overflows. Every unit melted, overflow included,
     * uses up water.
     */
    public MeltResult melt(AspectList aspects, double ratio, CrucibleSettings settings) {
        AspectList.Builder added = AspectList.builder().addAll(contents);
        int space = settings.capacity() - contents.total();
        int melted = 0;
        int overflow = 0;
        for (AspectStack stack : aspects.sortedByAmount()) {
            // A tiny margin keeps 4 * 0.75 at 3 despite floating point.
            int amount = (int) Math.floor(stack.amount() * ratio + 1e-9);
            int kept = Math.clamp(amount, 0, Math.max(space, 0));
            if (kept > 0) {
                added.add(stack.aspect(), kept);
            }
            space -= kept;
            overflow += amount - kept;
            melted += amount;
        }

        int used = essentiaSinceWaterDrop + melted;
        int drops = used / settings.essentiaPerWaterLevel();
        int newWater = water - drops;
        int left = newWater <= 0 ? 0 : used % settings.essentiaPerWaterLevel();
        return new MeltResult(new CrucibleTank(added.build(), newWater, left), overflow);
    }

    /** What opposite aspects in the tank wear down in one step; {@link AspectCancellation.Result#removed()} becomes Flux. */
    public CancelResult cancel(AspectRegistry registry, CrucibleSettings settings) {
        AspectCancellation.Result result = AspectCancellation.step(contents, registry, settings.cancelAmount());
        return new CancelResult(new CrucibleTank(result.remaining(), water, essentiaSinceWaterDrop), result.removed());
    }

    public record CancelResult(CrucibleTank tank, int flux) {}

    /** Saved with the block entity. */
    public static Codec<CrucibleTank> codec(Codec<AspectList> aspects) {
        return RecordCodecBuilder.create(i -> i.group(
                aspects.optionalFieldOf("contents", AspectList.empty()).forGetter(CrucibleTank::contents),
                Codec.intRange(0, MAX_WATER).optionalFieldOf("water", 0).forGetter(CrucibleTank::water),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("essentia_since_water_drop", 0).forGetter(CrucibleTank::essentiaSinceWaterDrop)
        ).apply(i, CrucibleTank::new));
    }
}
