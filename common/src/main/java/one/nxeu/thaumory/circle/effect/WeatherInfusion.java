package one.nxeu.thaumory.circle.effect;

import net.minecraft.server.level.ServerLevel;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/** Weather on an item, active: Aqua brings rain, Ignis clears the sky, Tempestas a thunderstorm. */
final class WeatherInfusion implements InfusionEffect {
    @Override
    public boolean active() {
        return true;
    }

    @Override
    public boolean canUse(InfusionContext context) {
        ServerLevel level = context.level();
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() && context.parameter().isPresent();
    }

    @Override
    public void use(InfusionContext context) {
        int duration = (int) Math.round(context.setting("ticks_per_level", 6000) * context.infusionLevel());
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
