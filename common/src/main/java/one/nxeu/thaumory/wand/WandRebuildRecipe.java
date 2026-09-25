package one.nxeu.thaumory.wand;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.alchemy.ThaumoryRecipes;
import one.nxeu.thaumory.item.ThaumoryComponents;

/**
 * A wand with two of the same caps, or one core, other than its own: the wand with those parts
 * instead (requirements §7.1). Everything else on it stays; the parts taken off are left in the
 * cells the new ones came from.
 */
public final class WandRebuildRecipe extends CustomRecipe {
    public static final WandRebuildRecipe INSTANCE = new WandRebuildRecipe();
    public static final MapCodec<WandRebuildRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, WandRebuildRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private WandRebuildRecipe() {}

    private static Optional<WandCrafting.Rebuild> rebuild(CraftingInput input) {
        return WandCrafting.rebuild(WandAssemblyRecipe.cells(input), WandAssemblyRecipe::role)
                .filter(rebuild -> !rebuild.part().equals(current(input, rebuild)));
    }

    private static WandBuild build(CraftingInput input, WandCrafting.Rebuild rebuild) {
        return input.getItem(rebuild.wand()).getOrDefault(ThaumoryComponents.WAND_BUILD.get(), WandBuild.DEFAULT);
    }

    /** The wand's own part of the kind being put on. */
    private static Identifier current(CraftingInput input, WandCrafting.Rebuild rebuild) {
        WandBuild build = build(input, rebuild);
        return rebuild.kind() == WandCrafting.Role.CAP ? build.cap() : build.core();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return rebuild(input).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return rebuild(input).map(rebuild -> {
            ItemStack wand = input.getItem(rebuild.wand()).copyWithCount(1);
            WandBuild build = build(input, rebuild);
            wand.set(ThaumoryComponents.WAND_BUILD.get(),
                    rebuild.kind() == WandCrafting.Role.CAP ? build.withCap(rebuild.part()) : build.withCore(rebuild.part()));
            return wand;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        rebuild(input).ifPresent(rebuild -> {
            Identifier old = current(input, rebuild);
            BuiltInRegistries.ITEM.getOptional(old).ifPresent(item -> rebuild.parts().forEach(cell -> remaining.set(cell, new ItemStack(item))));
        });
        return remaining;
    }

    @Override
    public RecipeSerializer<WandRebuildRecipe> getSerializer() {
        return ThaumoryRecipes.WAND_REBUILD_SERIALIZER.get();
    }
}
