package one.nxeu.thaumory.api.aspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.text.TextEffect;

/**
 * An element that items, circles and potions are made of.
 *
 * <p>A primal aspect has a fixed direction on the aspect hexagon. A compound aspect is made of two
 * other aspects. Opposites are not stored here; ask {@link AspectRegistry#opposite(Aspect)}.
 *
 * <p>A known aspect's name may be drawn with a {@link TextEffect}; see {@link #withNameEffect}.
 *
 * <p>Two aspects are equal when their ids are equal.
 */
public final class Aspect {
    private final Identifier id;
    private final int color;
    private final List<Aspect> components;
    private final double degrees;
    private final Optional<TextEffect> nameEffect;

    private Aspect(Identifier id, int color, List<Aspect> components, double degrees, Optional<TextEffect> nameEffect) {
        this.id = Objects.requireNonNull(id, "id");
        this.color = color & 0xFFFFFF;
        this.components = List.copyOf(components);
        this.degrees = degrees;
        this.nameEffect = Objects.requireNonNull(nameEffect, "nameEffect");
    }

    /**
     * Creates a primal aspect.
     *
     * @param color RGB color, e.g. {@code 0xC9483E}
     * @param degrees direction on the aspect hexagon, counter-clockwise from Ignis
     */
    public static Aspect primal(Identifier id, int color, double degrees) {
        return new Aspect(id, color, List.of(), normalize(degrees), Optional.empty());
    }

    /** Creates a compound aspect made of two different aspects. */
    public static Aspect compound(Identifier id, int color, Aspect first, Aspect second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.equals(second)) {
            throw new IllegalArgumentException(id + " cannot be made of " + first + " twice");
        }
        return new Aspect(id, color, List.of(first, second), Double.NaN, Optional.empty());
    }

    private static double normalize(double degrees) {
        double normalized = degrees % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    public Identifier id() {
        return id;
    }

    /** RGB color without alpha. */
    public int color() {
        return color;
    }

    public boolean isPrimal() {
        return components.isEmpty();
    }

    /** The two aspects this compound is made of, or an empty list for a primal aspect. */
    public List<Aspect> components() {
        return components;
    }

    /**
     * Direction on the aspect hexagon in degrees, in {@code [0, 360)}.
     *
     * @throws IllegalStateException for a compound aspect
     */
    public double degrees() {
        if (!isPrimal()) {
            throw new IllegalStateException(id + " is a compound aspect and has no fixed direction");
        }
        return degrees;
    }

    /**
     * The primal aspects this aspect finally breaks down into, in component order. A primal aspect
     * returns itself. The potion map draws a compound aspect as one segment per entry.
     */
    public List<Aspect> primalBreakdown() {
        if (isPrimal()) {
            return List.of(this);
        }
        List<Aspect> result = new ArrayList<>();
        for (Aspect component : components) {
            result.addAll(component.primalBreakdown());
        }
        return List.copyOf(result);
    }

    /** Sum of the unit vectors of {@link #primalBreakdown()}. */
    public AspectVector vector() {
        AspectVector sum = AspectVector.ZERO;
        for (Aspect primal : primalBreakdown()) {
            sum = sum.add(AspectVector.ofDegrees(primal.degrees));
        }
        return sum;
    }

    /** This aspect, with its name drawn with {@code effect} once the player knows it. */
    public Aspect withNameEffect(TextEffect effect) {
        return new Aspect(id, color, components, degrees, Optional.of(effect));
    }

    /** How the name moves or glows once known; empty for plain text. */
    public Optional<TextEffect> nameEffect() {
        return nameEffect;
    }

    public String translationKey() {
        return "aspect." + id.getNamespace() + "." + id.getPath();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Aspect aspect && id.equals(aspect.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
