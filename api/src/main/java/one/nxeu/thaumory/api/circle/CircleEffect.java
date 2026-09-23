package one.nxeu.thaumory.api.circle;

/**
 * What a magic circle does, written in code and registered by id in
 * {@link one.nxeu.thaumory.api.ThaumoryApi#circleEffects()}. Which runes start it, whether it is
 * triggered or sustained, and what it costs come from the datapack
 * ({@code data/<namespace>/thaumory/circle/*.json}), so the same effect can serve several
 * combinations.
 */
@FunctionalInterface
public interface CircleEffect {
    /**
     * A triggered circle calls this once per activation, after paying for it. A sustained circle
     * calls it once a second while it runs.
     */
    void apply(CircleContext context);

    /**
     * Called once when a sustained circle stops, for whatever reason: it was stopped, ran out of
     * Essentia, or its circle broke. Undo what should not outlive the circle here. The context's
     * {@link CircleContext#data() data} is cleared afterwards.
     */
    default void stop(CircleContext context) {}
}
