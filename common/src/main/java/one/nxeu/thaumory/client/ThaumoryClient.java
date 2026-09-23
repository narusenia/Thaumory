package one.nxeu.thaumory.client;

import com.mojang.logging.LogUtils;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.SplitPacketTransformer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;
import one.nxeu.thaumory.network.AspectSyncPayload;
import one.nxeu.thaumory.network.KnowledgeSyncPayload;
import org.slf4j.Logger;

public final class ThaumoryClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ThaumoryClient() {}

    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, AspectSyncPayload.TYPE, AspectSyncPayload.STREAM_CODEC,
                List.of(new SplitPacketTransformer()), (payload, context) -> context.queue(() -> {
                    ClientItemAspects.replace(payload.items());
                    LOGGER.info("Received aspects for {} items", payload.items().size());
                }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, KnowledgeSyncPayload.TYPE, KnowledgeSyncPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    ClientKnowledge.replace(payload.knowledge());
                    LOGGER.info("Received knowledge: {} scanned items, {} aspects",
                            payload.knowledge().scanned(PlayerKnowledge.ITEMS).size(), payload.knowledge().aspects().size());
                }));
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            ClientItemAspects.clear();
            ClientKnowledge.clear();
        });
        ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> appendAspects(stack.getItem(), lines, flag));
    }

    // Debug view until scanning decides what players may see (M1-8): advanced tooltips only.
    private static void appendAspects(Item item, List<Component> lines, TooltipFlag flag) {
        if (!flag.isAdvanced()) {
            return;
        }
        AspectList aspects = ClientItemAspects.get(item);
        if (aspects.isEmpty()) {
            return;
        }
        lines.add(Component.translatable("tooltip.thaumory.aspects"));
        for (AspectStack stack : aspects.sortedByAmount()) {
            lines.add(Component.literal("  ")
                    .append(Component.translatable(stack.aspect().translationKey()).withColor(stack.aspect().color()))
                    .append(" x" + stack.amount()));
        }
    }
}
