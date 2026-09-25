package one.nxeu.thaumory.item;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.infusion.InfusionText;
import one.nxeu.thaumory.sound.ThaumorySounds;
import one.nxeu.thaumory.text.ThaumoryText;

/**
 * The working tool for magic circles. Right-clicking a Core starts or stops a sustained circle, or
 * sets off a triggered one once, and says how it went on the action bar. With an item on the
 * Core's pedestal, it infuses that item instead. A sneaking right click takes the pedestal out.
 */
public final class WandItem extends Item {
    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level world = context.getLevel();
        if (!(world.getBlockEntity(pos) instanceof CircleCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        Player user = context.getPlayer();
        if (user != null && user.isShiftKeyDown()) {
            if (!core.hasPedestal()) {
                return InteractionResult.PASS;
            }
            if (world instanceof ServerLevel level) {
                core.removePedestal(user);
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        core.rescan();
        if (core.hasPedestal() && !core.pedestalItem().isEmpty() && core.infusesPedestalItem()) {
            if (world instanceof ServerLevel level && user != null) {
                infuse(level, core, user);
            }
            return InteractionResult.SUCCESS;
        }
        if (world instanceof ServerLevel level && user != null) {
            Player player = context.getPlayer();
            Outcome outcome = operate(core, player);
            MutableComponent message = Component.translatable("message.thaumory.wand." + outcome.key);
            boolean shaken = outcome == Outcome.MISFIRED || outcome == Outcome.OVERLOADED;
            player.sendOverlayMessage(shaken ? ThaumoryText.withEffect(message, TextEffect.SHAKE) : message);
            level.playSound(null, context.getClickedPos(), outcome.sound.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    private enum Outcome {
        STARTED("started", ThaumorySounds.CIRCLE_ACTIVATE),
        STOPPED("stopped", ThaumorySounds.CIRCLE_DEACTIVATE),
        TRIGGERED("triggered", () -> SoundEvents.EVOKER_CAST_SPELL),
        NO_RINGS("no_rings", () -> SoundEvents.FIRE_EXTINGUISH),
        NO_RESPONSE("no_response", () -> SoundEvents.FIRE_EXTINGUISH),
        MISFIRED("misfired", () -> SoundEvents.FIRE_EXTINGUISH),
        LOW_RANK("low_rank", () -> SoundEvents.FIRE_EXTINGUISH),
        OVERLOADED("overloaded", () -> SoundEvents.FIRE_EXTINGUISH),
        NO_TARGET("no_target", () -> SoundEvents.FIRE_EXTINGUISH),
        NO_ESSENTIA("no_essentia", () -> SoundEvents.FIRE_EXTINGUISH);

        final String key;
        final Supplier<SoundEvent> sound;

        Outcome(String key, Supplier<SoundEvent> sound) {
            this.key = key;
            this.sound = sound;
        }
    }

    private static void infuse(ServerLevel level, CircleCoreBlockEntity core, Player player) {
        core.rescan();
        CircleCoreBlockEntity.InfuseOutcome outcome = core.infuse(Optional.of(player));
        MutableComponent message = switch (outcome.result()) {
            case INFUSED -> Component.translatable("message.thaumory.infusion.infused", InfusionText.describe(outcome.infusion().orElseThrow()));
            case INFUSED_STONE -> Component.translatable("message.thaumory.infusion.stone", core.pedestalItem().getHoverName());
            case NOT_FOR_STONE -> Component.translatable("message.thaumory.infusion.not_for_stone");
            case FAILED -> ThaumoryText.withEffect(Component.translatable("message.thaumory.infusion.failed"), TextEffect.SHAKE);
            case NOT_INFUSABLE -> Component.translatable("message.thaumory.infusion.not_infusable");
            case NO_CAPACITY -> Component.translatable("message.thaumory.infusion.no_capacity");
            case ACTIVE_TAKEN -> Component.translatable("message.thaumory.infusion.active_taken");
            case NO_ROOM -> Component.translatable("message.thaumory.infusion.no_room", outcome.used(), outcome.capacity(),
                    outcome.infusion().orElseThrow().capacity());
            case NO_ITEM -> Component.translatable("message.thaumory.infusion.no_item");
            case NO_RINGS -> Component.translatable("message.thaumory.wand.no_rings");
            case UNDEFINED -> Component.translatable("message.thaumory.wand.no_response");
            case MISFIRED -> ThaumoryText.withEffect(Component.translatable("message.thaumory.wand.misfired"), TextEffect.SHAKE);
            case LOW_RANK -> Component.translatable("message.thaumory.wand.low_rank");
            case NO_ESSENTIA -> Component.translatable("message.thaumory.wand.no_essentia");
        };
        player.sendOverlayMessage(message);
        level.playSound(null, core.getBlockPos(), switch (outcome.result()) {
            case INFUSED, INFUSED_STONE -> ThaumorySounds.CIRCLE_INFUSE.get();
            case FAILED -> SoundEvents.GENERIC_EXTINGUISH_FIRE;
            default -> SoundEvents.FIRE_EXTINGUISH;
        }, SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    private static Outcome operate(CircleCoreBlockEntity core, Player player) {
        if (core.isRunning()) {
            core.stop();
            return Outcome.STOPPED;
        }
        core.rescan();
        return switch (core.start(Optional.of(player))) {
            case STARTED, ALREADY_RUNNING -> Outcome.STARTED;
            case NO_RINGS -> Outcome.NO_RINGS;
            case UNDEFINED -> Outcome.NO_RESPONSE;
            case MISFIRED -> Outcome.MISFIRED;
            case LOW_RANK -> Outcome.LOW_RANK;
            case OVERLOADED -> Outcome.OVERLOADED;
            case NO_ESSENTIA -> Outcome.NO_ESSENTIA;
            case TRIGGERED_ONLY -> switch (core.trigger(Optional.of(player))) {
                case TRIGGERED -> Outcome.TRIGGERED;
                case NO_RINGS -> Outcome.NO_RINGS;
                case UNDEFINED, SUSTAINED_ONLY -> Outcome.NO_RESPONSE;
                case MISFIRED -> Outcome.MISFIRED;
                case LOW_RANK -> Outcome.LOW_RANK;
                case OVERLOADED -> Outcome.OVERLOADED;
                case NO_TARGET -> Outcome.NO_TARGET;
                case NO_ESSENTIA -> Outcome.NO_ESSENTIA;
                case STOPPED -> Outcome.STOPPED;
            };
        };
    }
}
