package one.nxeu.thaumory.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import one.nxeu.thaumory.item.FluxCrystalItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A dropped Flux crystal breaks when it despawns or touches water (requirements §17.5). Falling out
 * of the world is in {@link EntityMixin}; burning and the like go through
 * {@link FluxCrystalItem#onDestroyed}.
 */
@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    /** The discards inside {@code tick}: an emptied stack, and one that has lain too long. */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void thaumory$shatterOnDespawn(CallbackInfo ci) {
        FluxCrystalItem.shatter(self());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void thaumory$shatterInWater(CallbackInfo ci) {
        ItemEntity self = self();
        if (!self.isRemoved() && self.isInWater() && self.getItem().getItem() instanceof FluxCrystalItem) {
            FluxCrystalItem.shatter(self);
            self.discard();
        }
    }

    private ItemEntity self() {
        return (ItemEntity) (Object) this;
    }
}
