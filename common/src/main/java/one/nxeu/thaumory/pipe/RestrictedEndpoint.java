package one.nxeu.thaumory.pipe;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;

/**
 * A container as a network reaches it through particular pipes (requirements §8.2): filter pipes
 * let only their aspect through, and a pump only draws.
 *
 * @param allowed     the aspects that may pass; empty means any
 * @param extractOnly whether nothing may go in (a Crucible through a pump)
 */
public record RestrictedEndpoint(PipeEndpoint endpoint, Optional<Set<Aspect>> allowed, boolean extractOnly) implements PipeEndpoint {
    /**
     * What the pipes touching a container let through together: each gives its filter, or empty
     * for a plain pipe (or a filter pipe with no filter), which lets anything through.
     */
    public static Optional<Set<Aspect>> union(List<Optional<Aspect>> filters) {
        Set<Aspect> aspects = new HashSet<>();
        for (Optional<Aspect> filter : filters) {
            if (filter.isEmpty()) {
                return Optional.empty();
            }
            aspects.add(filter.get());
        }
        return Optional.of(Set.copyOf(aspects));
    }

    private boolean passes(Aspect aspect) {
        return allowed.map(set -> set.contains(aspect)).orElse(true);
    }

    @Override
    public int priority() {
        return endpoint.priority();
    }

    /** Only what may pass, so nothing else is drawn. */
    @Override
    public AspectList contents() {
        AspectList contents = endpoint.contents();
        if (allowed.isEmpty()) {
            return contents;
        }
        AspectList.Builder passing = AspectList.builder();
        for (AspectStack stack : contents.stacks()) {
            if (passes(stack.aspect())) {
                passing.add(stack.aspect(), stack.amount());
            }
        }
        return passing.build();
    }

    @Override
    public int space(Aspect aspect) {
        return extractOnly || !passes(aspect) ? 0 : endpoint.space(aspect);
    }

    @Override
    public int insert(Aspect aspect, int max) {
        return extractOnly || !passes(aspect) ? 0 : endpoint.insert(aspect, max);
    }

    @Override
    public int extract(Aspect aspect, int max) {
        return passes(aspect) ? endpoint.extract(aspect, max) : 0;
    }
}
