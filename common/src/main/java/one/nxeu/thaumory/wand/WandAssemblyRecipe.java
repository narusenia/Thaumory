package one.nxeu.thaumory.wand;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
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
import one.nxeu.thaumory.item.ThaumoryItems;

/** Two of the same caps and a core in a diagonal line make a wand of those parts (requirements §7.1). */
public final class WandAssemblyRecipe extends CustomRecipe {
    public static final WandAssemblyRecipe INSTANCE = new WandAssemblyRecipe();
    public static final MapCodec<WandAssemblyRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, WandAssemblyRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private WandAssemblyRecipe() {}

    /** The grid's items by id, empty cells as empty. */
    static List<Optional<Identifier>> cells(CraftingInput input) {
        return input.items().stream()
                .map(stack -> stack.isEmpty() ? Optional.<Identifier>empty() : Optional.of(BuiltInRegistries.ITEM.getKey(stack.getItem())))
                .toList();
    }

    static WandCrafting.Role role(Identifier item) {
        return WandParts.role(item, BuiltInRegistries.ITEM.getKey(ThaumoryItems.WAND.get()));
    }

    private static Optional<WandCrafting.Assembly> assembly(CraftingInput input) {
        return WandCrafting.assembly(input.width(), input.height(), cells(input), WandAssemblyRecipe::role);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return assembly(input).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return assembly(input).map(assembly -> {
            ItemStack wand = new ItemStack(ThaumoryItems.WAND.get());
            wand.set(ThaumoryComponents.WAND_BUILD.get(), new WandBuild(assembly.cap(), assembly.core()));
            return wand;
        }).orElse(ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<WandAssemblyRecipe> getSerializer() {
        return ThaumoryRecipes.WAND_ASSEMBLY_SERIALIZER.get();
    }
}
