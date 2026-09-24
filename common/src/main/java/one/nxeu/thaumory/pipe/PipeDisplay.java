package one.nxeu.thaumory.pipe;

import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectColors;

/**
 * What a pipe shows of the Essentia in transit (requirements §8.2): the mixed color and how full
 * it looks, in four steps, so the client hears of it only when it visibly changes.
 *
 * @param level 0 when empty, else 1 to {@link #STEPS}
 */
public record PipeDisplay(int color, int level) {
    public static final int STEPS = 4;
    public static final PipeDisplay EMPTY = new PipeDisplay(0xFFFFFF, 0);

    public static PipeDisplay of(AspectList carried, int capacity) {
        int total = carried.total();
        if (total <= 0) {
            return EMPTY;
        }
        int level = (int) Math.round((double) STEPS * total / Math.max(1, capacity));
        return new PipeDisplay(AspectColors.mix(carried), Math.clamp(level, 1, STEPS));
    }
}
