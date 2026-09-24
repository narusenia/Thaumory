package one.nxeu.thaumory.circle.effect;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import one.nxeu.thaumory.api.circle.CircleContext;
import one.nxeu.thaumory.api.circle.CircleEffect;

/**
 * Ignis + Metallum, sustained. Cooks dropped items in range by the furnace's recipes, a few a
 * second, dropping what comes out where they lay along with the furnace's experience. What it has
 * cooked is marked so it is not cooked again (requirements §17.4).
 */
final class SmeltingEffect implements CircleEffect {
    /** Entity tag on items the circle made. */
    static final String SMELTED = "thaumory.smelted";

    @Override
    public void apply(CircleContext context) {
        ServerLevel level = context.level();
        int left = CircleRange.itemsPerCall(context);
        for (ItemEntity item : CircleRange.items(context, item -> !item.entityTags().contains(SMELTED))) {
            if (left <= 0) {
                return;
            }
            ItemStack stack = item.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) {
                continue;
            }
            ItemStack result = recipe.get().value().assemble(input);
            if (result.isEmpty()) {
                continue;
            }
            int count = Math.min(left, stack.getCount());
            left -= count;
            result.setCount(result.getCount() * count);
            stack.shrink(count);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            ItemEntity cooked = new ItemEntity(level, item.getX(), item.getY(), item.getZ(), result, 0, 0.1, 0);
            cooked.addTag(SMELTED);
            level.addFreshEntity(cooked);
            experience(level, cooked, count, recipe.get().value().experience());
            context.affected(cooked);
        }
    }

    /** As the furnace does it: the whole part, and the fraction left over as a chance of one more. */
    private static void experience(ServerLevel level, ItemEntity at, int count, float each) {
        float total = count * each;
        int reward = Mth.floor(total);
        if (level.getRandom().nextFloat() < Mth.frac(total)) {
            reward++;
        }
        ExperienceOrb.award(level, at.position(), reward);
    }
}
