package one.nxeu.thaumory.api.essentia;

import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;

/**
 * Something that holds Essentia and lets it be moved in and out: a Crucible, a jar, a circle's
 * Core, or an addon's block. Loader-neutral; each loader wraps it in its own transfer system (on
 * Fabric, {@code Storage<EssentiaVariant>} through the {@code thaumory:essentia} block lookup).
 *
 * <p>A transfer works on a pending copy of the contents and hands the result back through
 * {@link #update} once it is final, so the rules here are asked about contents that may not be
 * the container's own yet.
 */
public interface EssentiaContainer {
    /** What the container holds now. */
    AspectList contents();

    /** How much more of {@code aspect} would go in on top of {@code contents}. Zero if none. */
    int space(AspectList contents, Aspect aspect);

    /** Whether {@code aspect} may be taken out. */
    boolean canExtract(Aspect aspect);

    /**
     * Takes {@code contents} as what the container now holds, after a transfer became final. The
     * container may settle it further, such as opposites cancelling into Flux.
     */
    void update(AspectList contents);
}
