package one.nxeu.thaumory.client;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.wand.WandFoci;
import one.nxeu.thaumory.wand.WandFocus;
import one.nxeu.thaumory.wand.WandPart;
import one.nxeu.thaumory.wand.WandParts;

/** The wand parts and foci as the server last sent them, for tooltips and the focus menu. */
public final class ClientWandParts {
    private static volatile Map<Identifier, WandPart> parts = Map.of();
    private static volatile Map<Identifier, WandFocus> foci = Map.of();

    private ClientWandParts() {}

    public static void replace(List<WandPart> received, List<WandFocus> receivedFoci) {
        parts = WandParts.table(received);
        foci = WandFoci.table(receivedFoci);
    }

    public static Map<Identifier, WandFocus> foci() {
        return foci;
    }

    public static Map<Identifier, WandPart> get() {
        return parts;
    }
}
