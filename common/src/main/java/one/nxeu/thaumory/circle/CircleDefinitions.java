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
     * @param anyParameter whether slot 3 may hold any aspect
     * @param parameters   slot 3 contents accepted besides that, empty meaning no rune
     * @param settings     numbers for the effect, by name
     */
    public record Definition(Identifier id, Identifier effect, Aspect first, Aspect second, boolean anyParameter,
            Set<Optional<Aspect>> parameters, CircleMode mode, int cost, int interval, Map<String, Double> settings,
            InfusionCost infusionCost, int capacity, int itemCost, Map<String, Double> itemSettings) {
        public boolean matches(Aspect a, Aspect b, Optional<Aspect> parameter) {
            boolean runes = (first.equals(a) && second.equals(b)) || (first.equals(b) && second.equals(a));
            return runes && ((anyParameter && parameter.isPresent()) || parameters.contains(parameter));
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
            boolean any = false;
            Set<Optional<Aspect>> parameters = new HashSet<>();
            for (String slot3 : file.slot3()) {
                if (slot3.equals(CircleDefinitionFile.ANY)) {
                    any = true;
                } else if (slot3.equals(CircleDefinitionFile.NONE)) {
                    parameters.add(Optional.empty());
                } else {
                    Optional<Aspect> aspect = Optional.ofNullable(Identifier.tryParse(slot3)).flatMap(aspects::get);
                    if (aspect.isEmpty()) {
                        warn.accept("Skipping circle " + id + ": unknown slot 3 aspect " + slot3);
                        return;
                    }
                    parameters.add(aspect);
                }
            }
            definitions.add(new Definition(id, file.effect(), first.get(), second.get(), any,
                    Set.copyOf(parameters), file.mode(), file.cost(), file.interval(), Map.copyOf(file.settings()), file.infusionCost(),
                    file.capacity(), file.itemCost().orElse(file.cost()), Map.copyOf(file.itemSettings())));
        });
        return new CircleDefinitions(List.copyOf(definitions.reversed()));
    }

    public Optional<Definition> find(Aspect a, Aspect b, Optional<Aspect> parameter) {
        return definitions.stream().filter(d -> d.matches(a, b, parameter)).findFirst();
    }

    /** The combination that starts {@code effect}; the first one when several do. */
    public Optional<Definition> forEffect(Identifier effect) {
        return definitions.stream().filter(d -> d.effect().equals(effect)).findFirst();
    }

    public int size() {
        return definitions.size();
    }
}
