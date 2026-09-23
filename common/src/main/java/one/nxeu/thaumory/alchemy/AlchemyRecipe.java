package one.nxeu.thaumory.alchemy;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectCodecs;

/**
 * {@code "type": "thaumory:alchemy"}: a catalyst dropped into a boiling Crucible that holds the
 * aspects turns into the result, using up the aspects and the catalyst.
 *
 * <pre>{@code
 * {
 *   "type": "thaumory:alchemy",
 *   "catalyst": "minecraft:stone",
 *   "aspects": { "thaumory:terra": 8, "thaumory:arcanum": 4 },
 *   "result": { "id": "thaumory:blank_rune" }
 * }
 * }</pre>
 */
public record AlchemyRecipe(Ingredient catalyst, AspectList aspects, ItemStackTemplate result) implements Recipe<AlchemyInput> {
    public static final MapCodec<AlchemyRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("catalyst").forGetter(AlchemyRecipe::catalyst),
            AspectCodecs.strictAspectList(ThaumoryApi.aspects()).fieldOf("aspects").forGetter(AlchemyRecipe::aspects),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlchemyRecipe::result)
    ).apply(i, AlchemyRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AlchemyRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    @Override
    public boolean matches(AlchemyInput input, Level level) {
        return catalyst.test(input.catalyst()) && input.contents().containsAll(aspects);
    }

    @Override
    public ItemStack assemble(AlchemyInput input) {
        return result.create();
    }

    /** Alchemy happens in the world, never in a recipe book. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<AlchemyRecipe> getSerializer() {
        return ThaumoryRecipes.ALCHEMY_SERIALIZER.get();
    }

    @Override
    public RecipeType<AlchemyRecipe> getType() {
        return ThaumoryRecipes.ALCHEMY.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
