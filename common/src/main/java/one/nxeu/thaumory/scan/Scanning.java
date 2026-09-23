package one.nxeu.thaumory.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** What scanning an item teaches a player. */
public final class Scanning {
    private Scanning() {}

    /**
     * @param knowledge    after the scan
     * @param newlyScanned false when the item had been scanned before
     * @param revealed     aspects whose names this scan revealed, largest amount first
     */
    public record Result(PlayerKnowledge knowledge, boolean newlyScanned, List<Aspect> revealed) {}

    /**
     * Records the item and reveals each of its aspects that the player has now seen in enough
     * different scanned items. Items are counted with their current aspects, so an item scanned
     * again after the threshold was lowered can still reveal aspects.
     */
    public static Result scanItem(PlayerKnowledge knowledge, Identifier item,
            Function<Identifier, AspectList> aspectsOf, ScanSettings settings) {
        boolean newlyScanned = !knowledge.hasScanned(PlayerKnowledge.ITEMS, item);
        PlayerKnowledge result = knowledge.withScanned(PlayerKnowledge.ITEMS, item);

        List<Aspect> revealed = new ArrayList<>();
        for (AspectStack stack : aspectsOf.apply(item).sortedByAmount()) {
            Aspect aspect = stack.aspect();
            if (!result.knowsAspect(aspect.id()) && seenIn(result, aspect, aspectsOf) >= settings.revealAfter()) {
                result = result.withAspect(aspect.id());
                revealed.add(aspect);
            }
        }
        return new Result(result, newlyScanned, List.copyOf(revealed));
    }

    private static long seenIn(PlayerKnowledge knowledge, Aspect aspect, Function<Identifier, AspectList> aspectsOf) {
        return knowledge.scanned(PlayerKnowledge.ITEMS).stream()
                .filter(scanned -> aspectsOf.apply(scanned).contains(aspect))
                .count();
    }
}
