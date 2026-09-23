package one.nxeu.thaumory.pipe;

import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;

/** A container a pipe network touches, as the network moves Essentia in and out of it. */
public interface PipeEndpoint {
    /** Core 3, labeled jar 2, unlabeled jar or another mod's storage 1. Essentia only flows upwards. */
    int priority();

    AspectList contents();

    /** How much more of {@code aspect} it takes now. */
    int space(Aspect aspect);

    /** Puts in up to {@code max}; returns how much went in. */
    int insert(Aspect aspect, int max);

    /** Takes out up to {@code max}; returns how much came out. */
    int extract(Aspect aspect, int max);
}
