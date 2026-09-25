package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectRegistry;

/** Every circle combination from the datapacks, resolved against the registered aspects and effects. */
public final class CircleDefinitions {
    public static final CircleDefinitions EMPTY = new CircleDefinitions(List.of());

    /**
     * What a slot past the two effect runes accepts.
     *
     * @param any     whether the slot may hold any aspect
     * @param aspects contents accepted besides that, empty meaning no rune
     */
    public record Slot(boolean any, Set<Optional<Aspect>> aspects) {
        public Slot {
            aspects = Set.copyOf(aspects);
        }

        public boolean accepts(Optional<Aspect> rune) {
            return (any && rune.isPresent()) || aspects.contains(rune);
        }
    }

    /**
     * @param slot3    what slot 3 (the parameter) accepts
     * @param slot4    what slot 4 accepts
     * @param rank     the lowest Core rank the circle runs on; lower Cores still find it, but do not run it
     * @param settings numbers for the effect, by name
     */
    public record Definition(Identifier id, Identifier effect, Aspect first, Aspect second, Slot slot3, Slot slot4, int rank,
            CircleMode mode, int cost, int interval, Map<String, Double> settings, InfusionCost infusionCost, int capacity, int itemCost,
            Map<String, Double> itemSettings) {
        public boolean matches(Aspect a, Aspect b, Optional<Aspect> parameter, Optional<Aspect> fourth) {
            boolean runes = (first.equals(a) && second.equals(b)) || (first.equals(b) && second.equals(a));
            return runes && slot3.accepts(parameter) && slot4.accepts(fourth);
        }
    }

    /** Latest file id first, so it wins when two files define the same combination. */
    private final List<Definition> definitions;

    private CircleDefinitions(List<Definition> definitions) {
        this.definitions = definitions;
    }

    /** Skips, with a warning, files naming an effect that is not registered or an unknown aspect. */
    public static CircleDefinitions build(Map<Identifier, CircleDefinitionFile> files, AspectRegistry aspects,
            Predicate<Identifier> effectExists, Consumer<String> warn) {
        List<Definition> definitions = new ArrayList<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Identifier id = entry.getKey();
            CircleDefinitionFile file = entry.getValue();
            if (!effectExists.test(file.effect())) {
                warn.accept("Skipping circle " + id + ": unknown effect " + file.effect());
                return;
            }
            Optional<Aspect> first = aspects.get(file.runes().get(0));
            Optional<Aspect> second = aspects.get(file.runes().get(1));
            if (first.isEmpty() || second.isEmpty()) {
                warn.accept("Skipping circle " + id + ": unknown aspect in " + file.runes());
                return;
            }
            Optional<Slot> slot3 = slot(file.slot3(), aspects, unknown -> warn.accept("Skipping circle " + id + ": unknown slot 3 aspect " + unknown));
            Optional<Slot> slot4 = slot(file.slot4(), aspects, unknown -> warn.accept("Skipping circle " + id + ": unknown slot 4 aspect " + unknown));
            if (slot3.isEmpty() || slot4.isEmpty()) {
                return;
            }
            definitions.add(new Definition(id, file.effect(), first.get(), second.get(), slot3.get(), slot4.get(), file.rank(), file.mode(), file.cost(), file.interval(), Map.copyOf(file.settings()), file.infusionCost(),
                    file.capacity(), file.itemCost().orElse(file.cost()), Map.copyOf(file.itemSettings())));
        });
        return new CircleDefinitions(List.copyOf(definitions.reversed()));
    }

    /** Reads a slot's entries; empty, after telling {@code unknown}, when one names an unknown aspect. */
    private static Optional<Slot> slot(List<String> entries, AspectRegistry aspects, Consumer<String> unknown) {
        boolean any = false;
        Set<Optional<Aspect>> accepted = new HashSet<>();
        for (String entry : entries) {
            if (entry.equals(CircleDefinitionFile.ANY)) {
                any = true;
            } else if (entry.equals(CircleDefinitionFile.NONE)) {
                accepted.add(Optional.empty());
            } else {
                Optional<Aspect> aspect = Optional.ofNullable(Identifier.tryParse(entry)).flatMap(aspects::get);
                if (aspect.isEmpty()) {
                    unknown.accept(entry);
                    return Optional.empty();
                }
                accepted.add(aspect);
            }
        }
        return Optional.of(new Slot(any, accepted));
    }

    /** The combination with slot 4 empty. */
    public Optional<Definition> find(Aspect a, Aspect b, Optional<Aspect> parameter) {
        return find(a, b, parameter, Optional.empty());
    }

    /** Whatever rank the combination needs; the Core checks that itself. */
    public Optional<Definition> find(Aspect a, Aspect b, Optional<Aspect> parameter, Optional<Aspect> fourth) {
        return definitions.stream().filter(d -> d.matches(a, b, parameter, fourth)).findFirst();
    }

    /** The combination that starts {@code effect}; the first one when several do. */
    public Optional<Definition> forEffect(Identifier effect) {
        return definitions.stream().filter(d -> d.effect().equals(effect)).findFirst();
    }

    public int size() {
        return definitions.size();
    }
}
