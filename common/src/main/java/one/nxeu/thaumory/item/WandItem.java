package one.nxeu.thaumory.item;

import java.util.Optional;
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
import one.nxeu.thaumory.block.core.CoreBlockEntity;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.text.ThaumoryText;

/**
 * The working tool for magic circles. Right-clicking a Core starts or stops a sustained circle, or
 * sets off a triggered one once, and says how it went on the action bar.
 */
public final class WandItem extends Item {
    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof CoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
            Player player = context.getPlayer();
            Outcome outcome = operate(core, player);
            MutableComponent message = Component.translatable("message.thaumory.wand." + outcome.key);
            player.sendOverlayMessage(outcome == Outcome.MISFIRED ? ThaumoryText.withEffect(message, TextEffect.SHAKE) : message);
            level.playSound(null, context.getClickedPos(), outcome.sound, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    private enum Outcome {
        STARTED("started", SoundEvents.BEACON_ACTIVATE),
        STOPPED("stopped", SoundEvents.BEACON_DEACTIVATE),
        TRIGGERED("triggered", SoundEvents.EVOKER_CAST_SPELL),
        NO_RINGS("no_rings", SoundEvents.FIRE_EXTINGUISH),
        NO_RESPONSE("no_response", SoundEvents.FIRE_EXTINGUISH),
        MISFIRED("misfired", SoundEvents.FIRE_EXTINGUISH),
        NO_TARGET("no_target", SoundEvents.FIRE_EXTINGUISH),
        NO_ESSENTIA("no_essentia", SoundEvents.FIRE_EXTINGUISH);

        final String key;
        final SoundEvent sound;

        Outcome(String key, SoundEvent sound) {
            this.key = key;
            this.sound = sound;
        }
    }

    private static Outcome operate(CoreBlockEntity core, Player player) {
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
            case NO_ESSENTIA -> Outcome.NO_ESSENTIA;
            case TRIGGERED_ONLY -> switch (core.trigger(Optional.of(player))) {
                case TRIGGERED -> Outcome.TRIGGERED;
                case NO_RINGS -> Outcome.NO_RINGS;
                case UNDEFINED, SUSTAINED_ONLY -> Outcome.NO_RESPONSE;
                case MISFIRED -> Outcome.MISFIRED;
                case NO_TARGET -> Outcome.NO_TARGET;
                case NO_ESSENTIA -> Outcome.NO_ESSENTIA;
            };
        };
    }
}
