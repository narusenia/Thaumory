package one.nxeu.thaumory.essentia;

import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;

/**
 * An Essentia storage in the world as the loader sees it, Thaumory's or another mod's. Every
 * move is final at once; call only when no transfer of the loader's own is under way.
 */
public interface EssentiaHandle {
    AspectList contents();

    /** How much more of {@code aspect} would go in now. */
    int space(Aspect aspect);

    /** Returns how much went in. */
    int insert(Aspect aspect, int max);

    /** Returns how much came out. */
    int extract(Aspect aspect, int max);
}
