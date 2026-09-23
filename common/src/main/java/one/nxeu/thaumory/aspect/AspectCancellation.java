package one.nxeu.thaumory.aspect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;

/** Opposite aspects mixed together wear each other down (requirements §3). Used by the Crucible and by Jars. */
public final class AspectCancellation {
    private AspectCancellation() {}

    /**
     * @param remaining what is left
     * @param removed   the total amount lost, which becomes Flux
     */
    public record Result(AspectList remaining, int removed) {}

    /**
     * One step: every cancelling pair loses the same amount from both sides, up to {@code amount}
     * and never more than either side has left. An aspect in several pairs loses that much for
     * each of them, in the order the pairs are found.
     */
    public static Result step(AspectList aspects, AspectRegistry registry, int amount) {
        List<List<Aspect>> pairs = aspects.cancellingPairs(registry);
        if (pairs.isEmpty() || amount <= 0) {
            return new Result(aspects, 0);
        }
        Map<Aspect, Integer> left = new HashMap<>();
        for (AspectStack stack : aspects.stacks()) {
            left.put(stack.aspect(), stack.amount());
        }
        int removed = 0;
        for (List<Aspect> pair : pairs) {
            Aspect first = pair.get(0);
            Aspect second = pair.get(1);
            int taken = Math.min(amount, Math.min(left.get(first), left.get(second)));
            left.put(first, left.get(first) - taken);
            left.put(second, left.get(second) - taken);
            removed += taken * 2;
        }
        AspectList.Builder remaining = AspectList.builder();
        for (AspectStack stack : aspects.stacks()) {
            remaining.add(stack.aspect(), left.get(stack.aspect()));
        }
        return new Result(remaining.build(), removed);
    }

    /** Cancels until no opposite pair is left: each pair loses whatever its smaller side holds. */
    public static Result full(AspectList aspects, AspectRegistry registry) {
        AspectList current = aspects;
        int removed = 0;
        while (true) {
            Result step = step(current, registry, Integer.MAX_VALUE);
            if (step.removed() == 0) {
                return new Result(current, removed);
            }
            current = step.remaining();
            removed += step.removed();
        }
    }
}
