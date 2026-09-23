package one.nxeu.thaumory.essentia;

import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.essentia.EssentiaContainer;

/**
 * A container's contents while a transfer is under way: moves change this copy only, following
 * the container's rules, until the transfer is final and the copy goes back through
 * {@link EssentiaContainer#update}.
 */
public final class PendingEssentia {
    private final EssentiaContainer container;
    private AspectList contents;

    public PendingEssentia(EssentiaContainer container) {
        this.container = container;
        this.contents = container.contents();
    }

    public AspectList contents() {
        return contents;
    }

    /** Puts back an earlier copy, for a transfer that was called off. */
    public void reset(AspectList earlier) {
        contents = earlier;
    }

    /** Takes in up to {@code max} of {@code aspect}. Returns how much went in. */
    public int insert(Aspect aspect, int max) {
        int amount = Math.min(Math.max(0, max), container.space(contents, aspect));
        if (amount > 0) {
            contents = contents.plus(AspectList.of(aspect, amount));
        }
        return amount;
    }

    /** Takes out up to {@code max} of {@code aspect}. Returns how much came out. */
    public int extract(Aspect aspect, int max) {
        if (!container.canExtract(aspect)) {
            return 0;
        }
        int amount = Math.min(Math.max(0, max), contents.amount(aspect));
        if (amount > 0) {
            contents = contents.minus(AspectList.of(aspect, amount));
        }
        return amount;
    }

    /** Whether the copy differs from what the container holds, so there is something to hand back. */
    public boolean changed() {
        return !contents.equals(container.contents());
    }
}
