package one.nxeu.thaumory.api.aspect;

import java.util.Objects;

/** A positive amount of one aspect. */
public record AspectStack(Aspect aspect, int amount) {
    public AspectStack {
        Objects.requireNonNull(aspect, "aspect");
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + aspect + " x" + amount);
        }
    }

    @Override
    public String toString() {
        return aspect + " x" + amount;
    }
}
