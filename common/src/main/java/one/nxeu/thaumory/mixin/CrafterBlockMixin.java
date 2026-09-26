package one.nxeu.thaumory.mixin;

import java.util.Optional;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import one.nxeu.thaumory.research.ResearchProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A crafter has nobody's research, so it makes only recipes no chapter locks (requirements §6.2). */
@Mixin(CrafterBlock.class)
abstract class CrafterBlockMixin {
    @Inject(method = "getPotentialResults", at = @At("RETURN"), cancellable = true)
    private static void thaumory$onlyOpenRecipes(CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
        cir.setReturnValue(cir.getReturnValue().filter(holder -> ResearchProgress.canUse(Optional.empty(), holder.id().identifier())));
    }
}
