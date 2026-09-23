package one.nxeu.thaumory;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.aspect.data.ItemAspectReloadListener;
import one.nxeu.thaumory.command.ThaumoryCommands;

public final class Thaumory {
    public static final String MOD_ID = ThaumoryApi.MOD_ID;

    public static void init() {
        ThaumoryAspects.register(ThaumoryApi.aspects());

        ReloadListenerRegistry.register(PackType.SERVER_DATA, new ItemAspectReloadListener(), id("item_aspects"));
        CommandRegistrationEvent.EVENT.register(
                (dispatcher, context, selection) -> ThaumoryCommands.register(dispatcher, context));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
