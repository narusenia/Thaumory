package one.nxeu.thaumory.rune;

import java.util.Optional;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.jar.EssentiaTransfer;

/** Pouring a jar into a blank rune turns it into a rune of one aspect. */
public final class RuneInfusion {
    private RuneInfusion() {}

    /**
     * @param aspect    the rune's aspect
     * @param remaining what the jar holds afterwards
     */
    public record Result(Aspect aspect, AspectList remaining) {}

    /**
     * Takes {@code cost} of the aspect the jar would give when drawn from: its label's, or else
     * its largest. Empty when the jar holds less than that, and then nothing is taken.
     */
    public static Optional<Result> infuse(AspectList jar, Optional<Aspect> label, int cost) {
        return EssentiaTransfer.drawnAspect(jar, label)
                .filter(aspect -> jar.amount(aspect) >= cost)
                .map(aspect -> new Result(aspect, jar.minus(AspectList.of(aspect, cost))));
    }
}
