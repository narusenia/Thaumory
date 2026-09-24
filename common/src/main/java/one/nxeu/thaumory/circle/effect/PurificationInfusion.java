package one.nxeu.thaumory.circle.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ChunkPos;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;

/** Purification on an item, active: takes Flux out of the chunk the wearer stands in. */
final class PurificationInfusion implements InfusionEffect {
    @Override
    public boolean active() {
        return true;
    }

    @Override
    public void use(InfusionContext context) {
        double amount = context.setting("flux_per_level", 10) * context.infusionLevel();
        ThaumoryApi.flux().remove(context.level(), ChunkPos.containing(context.wearer().blockPosition()), amount);
        var pos = context.wearer().position();
        context.level().sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 20, 0.6, 0.6, 0.6, 0.02);
    }
}
