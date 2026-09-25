package one.nxeu.thaumory.client;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.wand.WandPart;
import one.nxeu.thaumory.wand.WandParts;

/** The wand parts as the server last sent them, for tooltips. */
public final class ClientWandParts {
    private static volatile Map<Identifier, WandPart> parts = Map.of();

    private ClientWandParts() {}

    public static void replace(List<WandPart> received) {
        parts = WandParts.table(received);
    }

    public static Map<Identifier, WandPart> get() {
        return parts;
    }
}
