package one.nxeu.thaumory;

import one.nxeu.thaumory.flux.FluxStorage;

/** Loader-specific implementations that each loader hands to {@link Thaumory#init}. */
public interface ThaumoryPlatform {
    FluxStorage fluxStorage();
}
