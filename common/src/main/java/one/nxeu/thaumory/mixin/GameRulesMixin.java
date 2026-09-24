package one.nxeu.thaumory.mixin;

import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import one.nxeu.thaumory.circle.effect.SafeguardEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reads mobGriefing as false for a mob standing in a safeguard circle. */
@Mixin(GameRules.class)
abstract class GameRulesMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void thaumory$safeguard(GameRule<?> rule, CallbackInfoReturnable<Object> cir) {
        if (rule == GameRules.MOB_GRIEFING && SafeguardEffect.stopsGriefing()) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
