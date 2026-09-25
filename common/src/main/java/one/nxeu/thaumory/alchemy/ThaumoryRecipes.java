package one.nxeu.thaumory.alchemy;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import one.nxeu.thaumory.wand.WandAssemblyRecipe;
import one.nxeu.thaumory.wand.WandRebuildRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.estimate.EstimationRecipe;
import one.nxeu.thaumory.api.estimate.RecipeAdapterRegistry;

public final class ThaumoryRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Thaumory.MOD_ID, Registries.RECIPE_TYPE);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Thaumory.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeType<AlchemyRecipe>> ALCHEMY = TYPES.register("alchemy", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return Thaumory.id("alchemy").toString();
        }
    });
    public static final RegistrySupplier<RecipeSerializer<AlchemyRecipe>> ALCHEMY_SERIALIZER = SERIALIZERS.register("alchemy",
            () -> new RecipeSerializer<>(AlchemyRecipe.CODEC, AlchemyRecipe.STREAM_CODEC));

    public static final RegistrySupplier<RecipeSerializer<WandAssemblyRecipe>> WAND_ASSEMBLY_SERIALIZER = SERIALIZERS.register("wand_assembly",
            () -> new RecipeSerializer<>(WandAssemblyRecipe.CODEC, WandAssemblyRecipe.STREAM_CODEC));
    public static final RegistrySupplier<RecipeSerializer<WandRebuildRecipe>> WAND_REBUILD_SERIALIZER = SERIALIZERS.register("wand_rebuild",
            () -> new RecipeSerializer<>(WandRebuildRecipe.CODEC, WandRebuildRecipe.STREAM_CODEC));

    private ThaumoryRecipes() {}

    public static void register() {
        TYPES.register();
        SERIALIZERS.register();
    }

    /** Alchemy results are what they are made of: the catalyst plus the aspects it used up. */
    public static void registerAdapters(RecipeAdapterRegistry registry) {
        registry.register(AlchemyRecipe.class, (id, recipe) -> {
            List<Identifier> catalysts = EstimationRecipe.slot(recipe.catalyst());
            if (catalysts.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new EstimationRecipe(id, List.of(catalysts),
                    BuiltInRegistries.ITEM.getKey(recipe.result().item().value()), recipe.result().count(), recipe.aspects()));
        });
    }
}
