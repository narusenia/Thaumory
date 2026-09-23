package one.nxeu.thaumory.client;

import com.mojang.logging.LogUtils;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.transformers.SplitPacketTransformer;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.client.codex.ArcaneCodexScreen;
import one.nxeu.thaumory.item.ArcaneCodexItem;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.jar.JarContents;
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
        ClientGuiEvent.RENDER_HUD.register(LoupeHud::render);
        ArcaneCodexItem.setScreenOpener(() -> Minecraft.getInstance().gui.setScreen(new ArcaneCodexScreen()));
        ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> {
            appendJarContents(stack, lines);
            appendAspects(stack.getItem(), lines);
        });
    }

    private static void appendJarContents(ItemStack stack, List<Component> lines) {
        JarContents contents = stack.get(ThaumoryComponents.JAR_CONTENTS.get());
        if (contents != null) {
            lines.addAll(JarText.describe(contents, OptionalInt.empty(), ClientKnowledge.get()));
        }
    }

    /** Aspects of scanned items only; aspects the player has not worked out show as "?". */
    private static void appendAspects(Item item, List<Component> lines) {
        PlayerKnowledge knowledge = ClientKnowledge.get();
        if (!knowledge.hasScanned(PlayerKnowledge.ITEMS, BuiltInRegistries.ITEM.getKey(item))) {
            return;
        }
        AspectList aspects = ClientItemAspects.get(item);
        if (aspects.isEmpty()) {
            return;
        }
        lines.add(Component.translatable("tooltip.thaumory.aspects").withColor(0xAAAAAA));
        for (AspectStack stack : aspects.sortedByAmount()) {
            lines.add(Component.literal("  ").append(AspectText.stack(stack, knowledge.knowsAspect(stack.aspect().id()))));
        }
    }
}
