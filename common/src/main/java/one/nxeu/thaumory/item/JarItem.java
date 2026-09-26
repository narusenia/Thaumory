package one.nxeu.thaumory.item;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.stone.CircleStoneBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.infusion.ItemEssentia;
import one.nxeu.thaumory.jar.EssentiaTransfer;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.rune.RuneInfusion;
import one.nxeu.thaumory.sound.ThaumorySounds;
import one.nxeu.thaumory.wand.WandCasting;

/**
 * A jar in hand. Right-clicking a Crucible, a placed jar or a circle's Core draws Essentia into it; sneaking pours
 * it out. Sneaking with a blank rune in the off hand pours into the rune instead, in the air or at
 * any other block. Anywhere else it places the jar, contents and all.
 */
public final class JarItem extends BlockItem {
    public JarItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity target = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(target instanceof CrucibleBlockEntity) && !(target instanceof JarBlockEntity) && !(target instanceof CircleCoreBlockEntity)
                && !(target instanceof CircleStoneBlockEntity)) {
            Player player = context.getPlayer();
            if (player != null && pouringIntoRune(player, context.getHand())) {
                return infuseRune(context.getLevel(), player, context.getItemInHand());
            }
            if (player != null && pouringIntoItem(player, context.getHand())) {
                return fillItem(context.getLevel(), player, context.getItemInHand());
            }
            return super.useOn(context);
        }
        if (context.getLevel() instanceof ServerLevel level) {
            transfer(level, context.getClickedPos(), target, context.getItemInHand(), context.isSecondaryUseActive());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (pouringIntoRune(player, hand)) {
            return infuseRune(level, player, player.getItemInHand(hand));
        }
        if (pouringIntoItem(player, hand)) {
            return fillItem(level, player, player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    private static boolean pouringIntoRune(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive()
                && player.getOffhandItem().is(ThaumoryItems.BLANK_RUNE.get());
    }

    /**
     * Pouring into an off-hand item whose active infusion stores Essentia (requirements §10.2), or into
     * an off-hand wand (§17.7).
     */
    private static boolean pouringIntoItem(Player player, InteractionHand hand) {
        ItemStack item = player.getOffhandItem();
        return hand == InteractionHand.MAIN_HAND && player.isSecondaryUseActive()
                && (!item.getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY).storedAspects().isEmpty()
                || item.is(ThaumoryItems.WAND.get()));
    }

    /** Pours what the off-hand item stores out of the jar, as much as fits; the rest stays in the jar. */
    private static InteractionResult fillItem(Level level, Player player, ItemStack jar) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack item = player.getOffhandItem();
        JarContents held = jar.getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY);
        AspectList taken;
        if (item.is(ThaumoryItems.WAND.get())) {
            taken = WandCasting.fill(item, held.aspects());
        } else {
            ItemEssentia.Fill fill = ItemEssentia.fill(item.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty()),
                    held.aspects(), item.get(ThaumoryComponents.INFUSIONS.get()).storedAspects(),
                    CircleCoreBlockEntity.settings().infusion().itemEssentia());
            taken = fill.taken();
            if (!taken.isEmpty()) {
                InfusionRuntime.setStored(item, fill.stored());
            }
        }
        if (taken.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.thaumory.infusion_use.nothing_to_pour"));
            return InteractionResult.FAIL;
        }
        JarContents updated = held.withAspects(held.aspects().minus(taken));
        if (updated.isEmpty()) {
            jar.remove(ThaumoryComponents.JAR_CONTENTS.get());
        } else {
            jar.set(ThaumoryComponents.JAR_CONTENTS.get(), updated);
        }
        serverLevel.playSound(null, player.blockPosition(), ThaumorySounds.JAR_USE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    /** Turns one blank rune in the off hand into a rune of the aspect the jar would give. */
    private static InteractionResult infuseRune(Level level, Player player, ItemStack jar) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        JarContents held = jar.getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY);
        int cost = RuneItem.cost();
        Optional<RuneInfusion.Result> result = RuneInfusion.infuse(held.aspects(), held.label(), cost);
        if (result.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.thaumory.rune.not_enough", cost));
            return InteractionResult.FAIL;
        }
        JarContents updated = held.withAspects(result.get().remaining());
        if (updated.isEmpty()) {
            jar.remove(ThaumoryComponents.JAR_CONTENTS.get());
        } else {
            jar.set(ThaumoryComponents.JAR_CONTENTS.get(), updated);
        }
        player.getOffhandItem().consume(1, player);
        player.getInventory().placeItemBackInInventory(RuneItem.of(result.get().aspect()), Prediction.SERVER_ONLY);
        serverLevel.playSound(null, player.blockPosition(), ThaumorySounds.JAR_USE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        serverLevel.playSound(null, player.blockPosition(), ThaumorySounds.RUNE_MADE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
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
        } else if (target instanceof CircleCoreBlockEntity core) {
            // A Core keeps each aspect apart, so nothing cancels and only its runes' aspects go in.
            result = pour
                    ? EssentiaTransfer.pourSeparated(held.aspects(), core.essentia(), core.acceptedAspects(), CircleCoreBlockEntity.capacity())
                    : EssentiaTransfer.draw(core.essentia(), held.aspects(), held.label(), capacity, registry);
            core.setEssentia(pour ? result.to() : result.from());
        } else if (target instanceof CircleStoneBlockEntity stone) {
            // Kept apart like a Core's, for the aspects of the circle burnt into it.
            result = pour
                    ? EssentiaTransfer.pourSeparated(held.aspects(), stone.essentia(), stone.acceptedAspects(), CircleStoneBlockEntity.capacity())
                    : EssentiaTransfer.draw(stone.essentia(), held.aspects(), held.label(), capacity, registry);
            stone.setEssentia(pour ? result.to() : result.from());
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
        level.playSound(null, pos, ThaumorySounds.JAR_USE.get(), SoundSource.BLOCKS, 1.0f, pour ? 0.9f : 1.1f);
    }
}
