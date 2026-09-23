package one.nxeu.thaumory.api.aspect;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * All known aspects, in registration order.
 *
 * <p>Register aspects during mod initialization. The registry is frozen once loading finishes,
 * after which {@link #register(Aspect)} throws.
 */
public final class AspectRegistry {
    private static final double DIRECTION_EPSILON = 1e-6;

    private final Map<Identifier, Aspect> aspects = new LinkedHashMap<>();
    private boolean frozen;

    /**
     * Adds an aspect.
     *
     * @throws IllegalStateException if the registry is frozen
     * @throws IllegalArgumentException if the id is taken, a primal already points the same way, a
     *     component is unregistered, the components are opposites, or another compound already has
     *     the same components
     */
    public Aspect register(Aspect aspect) {
        if (frozen) {
            throw new IllegalStateException("Aspect registry is frozen; cannot register " + aspect);
        }
        if (aspects.containsKey(aspect.id())) {
            throw new IllegalArgumentException("Duplicate aspect id " + aspect.id());
        }
        if (aspect.isPrimal()) {
            checkDirectionFree(aspect);
        } else {
            checkComponents(aspect);
        }
        aspects.put(aspect.id(), aspect);
        return aspect;
    }

    private void checkDirectionFree(Aspect primal) {
        for (Aspect existing : primals()) {
            if (sameDirection(existing.degrees(), primal.degrees())) {
                throw new IllegalArgumentException(
                        primal + " points the same way as " + existing + " (" + primal.degrees() + "°)");
            }
        }
    }

    private void checkComponents(Aspect compound) {
        List<Aspect> components = compound.components();
        for (Aspect component : components) {
            if (!aspects.containsKey(component.id())) {
                throw new IllegalArgumentException(compound + " uses unregistered aspect " + component);
            }
        }
        if (areOpposite(components.get(0), components.get(1))) {
            throw new IllegalArgumentException(
                    compound + " combines opposites " + components.get(0) + " and " + components.get(1));
        }
        Set<Aspect> recipe = Set.copyOf(components);
        for (Aspect existing : aspects.values()) {
            if (!existing.isPrimal() && Set.copyOf(existing.components()).equals(recipe)) {
                throw new IllegalArgumentException(compound + " has the same components as " + existing);
            }
        }
    }

    public Optional<Aspect> get(Identifier id) {
        return Optional.ofNullable(aspects.get(id));
    }

    /** Every aspect in registration order. */
    public Collection<Aspect> all() {
        return Collections.unmodifiableCollection(aspects.values());
    }

    /** Primal aspects in registration order. */
    public List<Aspect> primals() {
        return aspects.values().stream().filter(Aspect::isPrimal).toList();
    }

    /**
     * The aspect that cancels this one out.
     *
     * <p>A primal's opposite is the primal pointing the other way. A compound's opposite is the
     * compound made of its components' opposites. Empty when no such aspect is registered.
     */
    public Optional<Aspect> opposite(Aspect aspect) {
        if (aspect.isPrimal()) {
            double target = aspect.degrees() + 180;
            return primals().stream().filter(p -> sameDirection(p.degrees(), target)).findFirst();
        }
        Optional<Aspect> first = opposite(aspect.components().get(0));
        Optional<Aspect> second = opposite(aspect.components().get(1));
        if (first.isEmpty() || second.isEmpty()) {
            return Optional.empty();
        }
        Set<Aspect> recipe = Set.of(first.get(), second.get());
        return aspects.values().stream()
                .filter(a -> !a.isPrimal() && Set.copyOf(a.components()).equals(recipe))
                .findFirst();
    }

    public boolean areOpposite(Aspect first, Aspect second) {
        return opposite(first).map(second::equals).orElse(false);
    }

    /**
     * True if the two aspects cancel each other out when mixed: some primal in one's breakdown is
     * the opposite of some primal in the other's. For example Lux (Ignis + Aer) cancels with Aqua.
     */
    public boolean cancels(Aspect first, Aspect second) {
        for (Aspect a : first.primalBreakdown()) {
            for (Aspect b : second.primalBreakdown()) {
                if (areOpposite(a, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void freeze() {
        frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    private static boolean sameDirection(double a, double b) {
        double diff = Math.abs(a - b) % 360;
        return Math.min(diff, 360 - diff) < DIRECTION_EPSILON;
    }
}
