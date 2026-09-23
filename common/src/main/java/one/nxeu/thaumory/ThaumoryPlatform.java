package one.nxeu.thaumory;

import one.nxeu.thaumory.flux.FluxStorage;
import one.nxeu.thaumory.knowledge.KnowledgeStorage;

/** Loader-specific implementations that each loader hands to {@link Thaumory#init}. */
public interface ThaumoryPlatform {
    FluxStorage fluxStorage();

    KnowledgeStorage knowledgeStorage();
}
