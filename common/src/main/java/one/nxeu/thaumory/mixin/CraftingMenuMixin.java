package one.nxeu.thaumory.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import one.nxeu.thaumory.research.ResearchProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Crafting grids, the table's and the inventory's, find no recipe a chapter locks away from the
 * player (requirements §6.2): its result never shows.
 */
@Mixin(CraftingMenu.class)
abstract class CraftingMenuMixin {
    @ModifyExpressionValue(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;"))
    private static Optional<RecipeHolder<CraftingRecipe>> thaumory$lockedByChapter(Optional<RecipeHolder<CraftingRecipe>> found,
            @Local(argsOnly = true) Player player) {
        Optional<ServerPlayer> crafter = player instanceof ServerPlayer server ? Optional.of(server) : Optional.empty();
        return found.filter(holder -> ResearchProgress.canUse(crafter, holder.id().identifier()));
    }
}
