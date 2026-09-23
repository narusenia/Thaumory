package one.nxeu.thaumory.api.flux;

/**
 * How badly a chunk is polluted, from least to worst. The amount each stage starts at comes from
 * datapacks; what each stage does is fixed in code.
 */
public enum FluxStage {
    NONE,
    /** Purple particles; the book warns the player. */
    STAGNATION,
    /** Nearby blocks turn into polluted blocks and crops stop growing. */
    EROSION,
    /** Hostile mobs born from Flux spawn, and circles become less stable. */
    MANIFESTATION,
    /** Circles in range may misfire. */
    OVERLOAD
}
