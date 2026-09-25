package one.nxeu.thaumory.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import one.nxeu.thaumory.item.FluxCrystalItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A dropped Flux crystal that falls out of the world breaks (requirements §17.5). */
@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "onBelowWorld", at = @At("HEAD"))
    private void thaumory$shatterBelowWorld(CallbackInfo ci) {
        if ((Object) this instanceof ItemEntity item) {
            FluxCrystalItem.shatter(item);
        }
    }
}
