package one.nxeu.thaumory.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.jar.EssentiaTransfer;
import one.nxeu.thaumory.jar.JarContents;

/**
 * A jar in hand. Right-clicking a Crucible or a placed jar draws Essentia into it; sneaking pours
 * it out. Anywhere else it places the jar, contents and all.
 */
public final class JarItem extends BlockItem {
    public JarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity target = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(target instanceof CrucibleBlockEntity) && !(target instanceof JarBlockEntity)) {
            return super.useOn(context);
        }
        if (context.getLevel() instanceof ServerLevel level) {
            transfer(level, context.getClickedPos(), target, context.getItemInHand(), context.isSecondaryUseActive());
        }
        return InteractionResult.SUCCESS;
    }

    private static void transfer(ServerLevel level, BlockPos pos, BlockEntity target, ItemStack stack, boolean pour) {
        AspectRegistry registry = ThaumoryApi.aspects();
        int capacity = JarBlockEntity.capacity();
        JarContents held = stack.getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY);
        EssentiaTransfer.Result result;
        if (target instanceof CrucibleBlockEntity crucible) {
            AspectList tank = crucible.tank().contents();
            result = pour
                    ? EssentiaTransfer.pour(held.aspects(), tank, Optional.empty(), crucible.capacity(), true, false, registry)
                    : EssentiaTransfer.draw(tank, held.aspects(), held.label(), capacity, registry);
            crucible.setContents(pour ? result.to() : result.from());
        } else if (target instanceof JarBlockEntity jar) {
            JarContents placed = jar.contents();
            result = pour
                    ? EssentiaTransfer.pour(held.aspects(), placed.aspects(), placed.label(), capacity, false, true, registry)
                    : EssentiaTransfer.draw(placed.aspects(), held.aspects(), held.label(), capacity, registry);
            jar.setContents(placed.withAspects(pour ? result.to() : result.from()));
        } else {
            return;
        }

        AspectList heldAfter = pour ? result.from() : result.to();
        if (heldAfter.equals(held.aspects()) && result.flux() == 0) {
            return;
        }
        JarContents updated = held.withAspects(heldAfter);
        if (updated.isEmpty()) {
            stack.remove(ThaumoryComponents.JAR_CONTENTS.get());
        } else {
            stack.set(ThaumoryComponents.JAR_CONTENTS.get(), updated);
        }
        if (result.flux() > 0) {
            ThaumoryApi.flux().add(level, ChunkPos.containing(pos), result.flux());
        }
        level.playSound(null, pos, pour ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
    }
}
