package one.nxeu.thaumory.research;

import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** What research conditions may ask about a player: all of it comes from their knowledge. */
public interface ResearchFacts {
    Set<Identifier> scannedItems();

    /** Whether any scanned item is in the item tag {@code tag}. */
    boolean scannedAnyIn(Identifier tag);

    Set<Identifier> knownAspects();

    /** Combinations recorded with {@code success} (or failure), counting only {@code effect}'s if given. */
    int circles(boolean success, Optional<Identifier> effect);
}
