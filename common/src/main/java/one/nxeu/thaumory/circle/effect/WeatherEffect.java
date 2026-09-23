package one.nxeu.thaumory.circle.effect;

import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/** Tempestas + Arcanum, triggered. Aqua brings rain, Ignis clears the sky, Tempestas a thunderstorm. */
final class WeatherEffect implements CircleEffect {
    private static final int TICKS_PER_STRENGTH = 6000;

    /** Only under an open sky; a dimension without weather has nothing to change. */
    @Override
    public boolean canApply(CircleContext context) {
        return context.level().dimensionType().hasSkyLight() && !context.level().dimensionType().hasCeiling()
                && context.parameter().isPresent();
    }

    @Override
    public void apply(CircleContext context) {
        int duration = (int) Math.round(TICKS_PER_STRENGTH * context.strength());
        var server = context.level().getServer();
        context.parameter().ifPresent(aspect -> {
            if (aspect.equals(ThaumoryAspects.AQUA)) {
                server.setWeatherParameters(0, duration, true, false);
            } else if (aspect.equals(ThaumoryAspects.TEMPESTAS)) {
                server.setWeatherParameters(0, duration, true, true);
            } else if (aspect.equals(ThaumoryAspects.IGNIS)) {
                server.setWeatherParameters(duration, 0, false, false);
            }
        });
    }
}
