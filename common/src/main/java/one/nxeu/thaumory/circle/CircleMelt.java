package one.nxeu.thaumory.circle;

import java.util.Optional;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;

/**
 * What the melting circle makes of one item (requirements §17.4): the Crucible's share of each
 * aspect, rounded down; the aspect in slot 3 goes into the Core as far as there is room, and the
 * rest comes out as Flux, one for one.
 */
public final class CircleMelt {
    private CircleMelt() {}

    /** @param stored how much of the kept aspect goes into the Core; {@code flux} the rest */
    public record Result(int stored, int flux) {}

    /**
     * @param aspects the item's aspects
     * @param ratio   the share of each kept, as in the Crucible
     * @param kept    the aspect the Core takes, if any
     * @param room    how much more of it the Core holds
     */
    public static Result melt(AspectList aspects, double ratio, Optional<Aspect> kept, int room) {
        int stored = 0;
        int flux = 0;
        for (AspectStack stack : aspects.stacks()) {
            int amount = (int) Math.floor(stack.amount() * ratio + 1e-9);
            if (kept.filter(stack.aspect()::equals).isPresent()) {
                int into = Math.min(amount, Math.max(0, room));
                stored += into;
                flux += amount - into;
            } else {
                flux += amount;
            }
        }
        return new Result(stored, flux);
    }
}
