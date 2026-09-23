package one.nxeu.thaumory.jar;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.AspectCancellation;

/** Moving Essentia between containers: drawing into a jar and pouring out of one. */
public final class EssentiaTransfer {
    private EssentiaTransfer() {}

    /**
     * @param from what the giver holds afterwards
     * @param to   what the receiver holds afterwards
     * @param flux Essentia lost to cancelling or spilling
     */
    public record Result(AspectList from, AspectList to, int flux) {
        public boolean moved(AspectList before) {
            return !from.equals(before);
        }
    }

    /** The aspect a jar would draw from {@code from}: its label's, or else the largest (ties go to the lowest id). */
    public static Optional<Aspect> drawnAspect(AspectList from, Optional<Aspect> label) {
        if (label.isPresent()) {
            return from.contains(label.get()) ? label : Optional.empty();
        }
        return from.stacks().stream()
                .max(Comparator.comparingInt(AspectStack::amount)
                        .thenComparing(stack -> stack.aspect().id(), Comparator.reverseOrder()))
                .map(AspectStack::aspect);
    }

    /**
     * Draws one aspect from {@code from} into a jar holding {@code to}, as much as fits. An
     * unlabeled jar then cancels any opposites it now holds.
     */
    public static Result draw(AspectList from, AspectList to, Optional<Aspect> label, int capacity, AspectRegistry registry) {
        Optional<Aspect> aspect = drawnAspect(from, label);
        int space = capacity - to.total();
        if (aspect.isEmpty() || space <= 0) {
            return new Result(from, to, 0);
        }
        int amount = Math.min(from.amount(aspect.get()), space);
        AspectList moved = AspectList.of(aspect.get(), amount);
        return settle(from.minus(moved), to.plus(moved), label, 0, registry);
    }

    /**
     * Pours everything {@code label} lets through from {@code from} into {@code to}, largest
     * first. What does not fit stays behind, or with {@code spill} becomes Flux (a Crucible).
     * With {@code cancel}, the receiver cancels opposites at once (an unlabeled jar).
     */
    public static Result pour(AspectList from, AspectList to, Optional<Aspect> label, int capacity,
            boolean spill, boolean cancel, AspectRegistry registry) {
        int space = Math.max(0, capacity - to.total());
        AspectList.Builder accepted = AspectList.builder();
        AspectList.Builder moved = AspectList.builder();
        int overflow = 0;
        for (AspectStack stack : from.sortedByAmount()) {
            if (label.isPresent() && !label.get().equals(stack.aspect())) {
                continue;
            }
            int kept = Math.min(stack.amount(), space);
            accepted.add(stack.aspect(), stack.amount());
            moved.add(stack.aspect(), kept);
            space -= kept;
            overflow += stack.amount() - kept;
        }
        AspectList movedList = moved.build();
        AspectList received = to.plus(movedList);
        AspectList left = spill ? from.minus(accepted.build()) : from.minus(movedList);
        int spilled = spill ? overflow : 0;
        if (cancel && label.isEmpty()) {
            return settle(left, received, label, spilled, registry);
        }
        return new Result(left, received, spilled);
    }

    /**
     * Pours into a container that keeps each aspect apart, such as a circle's Core: only
     * {@code accepted} aspects move, each up to {@code perAspect}, and nothing cancels. What does
     * not fit stays behind.
     */
    public static Result pourSeparated(AspectList from, AspectList to, Set<Aspect> accepted, int perAspect) {
        AspectList.Builder moved = AspectList.builder();
        for (AspectStack stack : from.stacks()) {
            if (accepted.contains(stack.aspect())) {
                int space = Math.max(0, perAspect - to.amount(stack.aspect()));
                moved.add(stack.aspect(), Math.min(space, stack.amount()));
            }
        }
        AspectList movedList = moved.build();
        return new Result(from.minus(movedList), to.plus(movedList), 0);
    }

    /** Unlabeled jars cancel opposites the moment they mix; a labeled jar only ever holds one aspect. */
    private static Result settle(AspectList from, AspectList to, Optional<Aspect> label, int flux, AspectRegistry registry) {
        if (label.isPresent()) {
            return new Result(from, to, flux);
        }
        AspectCancellation.Result cancelled = AspectCancellation.full(to, registry);
        return new Result(from, cancelled.remaining(), flux + cancelled.removed());
    }
}
