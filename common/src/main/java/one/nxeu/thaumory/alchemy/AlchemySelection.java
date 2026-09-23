package one.nxeu.thaumory.alchemy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;

/** Which alchemy recipe a catalyst completes, given what is in the Crucible. */
public final class AlchemySelection {
    private AlchemySelection() {}

    /** A recipe whose catalyst matched, reduced to what selection needs. */
    public record Candidate<T>(Identifier id, AspectList aspects, T recipe) {}

    /**
     * Of the candidates the contents can pay for, the one needing the most Essentia in total;
     * ties go to the lowest recipe id.
     */
    public static <T> Optional<Candidate<T>> choose(List<Candidate<T>> candidates, AspectList contents) {
        return candidates.stream()
                .filter(candidate -> contents.containsAll(candidate.aspects()))
                .min(Comparator.<Candidate<T>>comparingInt(candidate -> -candidate.aspects().total())
                        .thenComparing(Candidate::id));
    }
}
