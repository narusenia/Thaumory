package one.nxeu.thaumory.item;

import com.mojang.logging.LogUtils;
import dev.architectury.networking.NetworkManager;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.text.TextEffect;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.Transcript;
import one.nxeu.thaumory.knowledge.Transcript.AspectTranscript;
import one.nxeu.thaumory.knowledge.Transcript.CircleTranscript;
import one.nxeu.thaumory.network.TranscribePayload;
import one.nxeu.thaumory.text.ThaumoryText;
import org.slf4j.Logger;

/**
 * A piece of knowledge copied out of the book (requirements §6.3). Written with paper and an ink
 * sac from the book's "transcribe" button; reading it teaches its holder and uses it up.
 */
public final class TranscriptItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TranscriptItem(Properties properties) {
        super(properties);
    }

    /** Receives the book's "transcribe" requests. */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TranscribePayload.TYPE, TranscribePayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        transcribe(player, payload.transcript());
                    }
                }));
    }

    public static ItemStack of(Transcript transcript) {
        ItemStack stack = new ItemStack(ThaumoryItems.TRANSCRIPT.get());
        stack.set(ThaumoryComponents.TRANSCRIPT.get(), transcript);
        return stack;
    }

    public static Optional<Transcript> transcript(ItemStack stack) {
        return Optional.ofNullable(stack.get(ThaumoryComponents.TRANSCRIPT.get()));
    }

    /** Copies knowledge the player holds, using up paper and an ink sac unless they need no materials. */
    public static void transcribe(ServerPlayer player, Transcript transcript) {
        if (!transcript.knownBy(Thaumory.knowledge().get(player))) {
            LOGGER.warn("{} asked to transcribe {} without knowing it", player.getName().getString(), transcript);
            return;
        }
        if (!player.hasInfiniteMaterials()) {
            Inventory inventory = player.getInventory();
            int paper = findSlot(inventory, Items.PAPER);
            int ink = findSlot(inventory, Items.INK_SAC);
            if (paper < 0 || ink < 0) {
                player.sendOverlayMessage(Component.translatable("message.thaumory.transcript.materials"));
                return;
            }
            inventory.removeItem(paper, 1);
            inventory.removeItem(ink, 1);
        }
        player.getInventory().placeItemBackInInventory(of(transcript), Prediction.SERVER_ONLY);
        player.sendOverlayMessage(Component.translatable("message.thaumory.transcript.written", describe(transcript, true)));
    }

    private static int findSlot(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Optional<Transcript> transcript = transcript(stack);
        if (transcript.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (transcript.get().knownBy(Thaumory.knowledge().get(serverPlayer))) {
                serverPlayer.sendOverlayMessage(Component.translatable("message.thaumory.transcript.known"));
                return InteractionResult.FAIL;
            }
            Thaumory.knowledge().update(serverPlayer, transcript.get()::teach);
            serverPlayer.sendSystemMessage(Component.translatable("message.thaumory.transcript.learned", describe(transcript.get(), true)));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return transcript(stack)
                .map(transcript -> (Component) Component.translatable(getDescriptionId() + "." + transcript.kind().getSerializedName()))
                .orElseGet(() -> super.getName(stack));
    }

    /**
     * What the transcript holds, for one who knows it: the aspect's name, or the circle's effect.
     * An aspect or effect no longer registered shows its id.
     */
    public static MutableComponent describe(Transcript transcript, boolean streak) {
        return switch (transcript) {
            case AspectTranscript(var id) -> ThaumoryApi.aspects().get(id)
                    .map(aspect -> streak ? AspectText.name(aspect, TextEffect.STREAK) : AspectText.name(aspect, true))
                    .orElseGet(() -> Component.literal(id.toString()));
            case CircleTranscript(CircleCombination combination) -> effectName(combination)
                    .map(name -> streak ? ThaumoryText.withEffect(name, TextEffect.STREAK) : name)
                    .orElseGet(() -> Component.literal(combination.toString()));
        };
    }

    private static Optional<MutableComponent> effectName(CircleCombination combination) {
        Optional<Aspect> first = ThaumoryApi.aspects().get(combination.first());
        Optional<Aspect> second = ThaumoryApi.aspects().get(combination.second());
        if (first.isEmpty() || second.isEmpty()) {
            return Optional.empty();
        }
        Optional<Aspect> parameter = combination.parameter().flatMap(ThaumoryApi.aspects()::get);
        return CircleDefinitionReloadListener.definitions().find(first.get(), second.get(), parameter)
                .map(definition -> Component.translatable(definition.effect().toLanguageKey("circle_effect")).withColor(0xCC99FF));
    }
}
