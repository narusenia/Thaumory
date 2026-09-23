package one.nxeu.thaumory.circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectRegistry;

/** Every circle combination from the datapacks, resolved against the registered aspects and effects. */
public final class CircleDefinitions {
    public static final CircleDefinitions EMPTY = new CircleDefinitions(List.of());

    /**
     * @param parameters slot 3 runes accepted, empty meaning no rune; unused when {@code anyParameter}
     */
    public record Definition(Identifier id, Identifier effect, Aspect first, Aspect second, boolean anyParameter,
            Set<Optional<Aspect>> parameters, CircleMode mode, int cost, int interval) {
        public boolean matches(Aspect a, Aspect b, Optional<Aspect> parameter) {
            boolean runes = (first.equals(a) && second.equals(b)) || (first.equals(b) && second.equals(a));
            return runes && (anyParameter ? parameter.isPresent() : parameters.contains(parameter));
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
            Set<Optional<Aspect>> parameters = Set.of();
            if (file.slot3().isPresent()) {
                List<Optional<Identifier>> unknown = file.slot3().get().stream()
                        .filter(p -> p.isPresent() && aspects.get(p.get()).isEmpty()).toList();
                if (!unknown.isEmpty()) {
                    warn.accept("Skipping circle " + id + ": unknown slot 3 aspect in " + file.slot3().get());
                    return;
                }
                parameters = file.slot3().get().stream().map(p -> p.flatMap(aspects::get)).collect(Collectors.toUnmodifiableSet());
            }
            definitions.add(new Definition(id, file.effect(), first.get(), second.get(), file.slot3().isEmpty(),
                    parameters, file.mode(), file.cost(), file.interval()));
        });
        return new CircleDefinitions(List.copyOf(definitions.reversed()));
    }

    public Optional<Definition> find(Aspect a, Aspect b, Optional<Aspect> parameter) {
        return definitions.stream().filter(d -> d.matches(a, b, parameter)).findFirst();
    }

    public int size() {
        return definitions.size();
    }
}
