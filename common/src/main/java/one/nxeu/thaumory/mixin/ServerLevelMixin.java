package one.nxeu.thaumory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import one.nxeu.thaumory.circle.effect.SafeguardEffect;
import org.spongepowered.asm.mixin.Mixin;

/** Remembers which entity is ticking, so a safeguard circle can answer its mobGriefing checks. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @WrapMethod(method = "tickNonPassenger")
    private void thaumory$trackTicking(Entity entity, Operation<Void> original) {
        Entity previous = SafeguardEffect.enterTick(entity);
        try {
            original.call(entity);
        } finally {
            SafeguardEffect.exitTick(previous);
        }
    }

    @WrapMethod(method = "tickPassenger")
    private void thaumory$trackTickingPassenger(Entity vehicle, Entity passenger, Operation<Void> original) {
        Entity previous = SafeguardEffect.enterTick(passenger);
        try {
            original.call(vehicle, passenger);
        } finally {
            SafeguardEffect.exitTick(previous);
        }
    }
}
