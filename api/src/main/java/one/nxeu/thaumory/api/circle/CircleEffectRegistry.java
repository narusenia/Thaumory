package one.nxeu.thaumory.api.circle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Every circle effect by id. Register during mod initialization, before datapacks load. */
public final class CircleEffectRegistry {
    private final Map<Identifier, CircleEffect> effects = new LinkedHashMap<>();

    public CircleEffect register(Identifier id, CircleEffect effect) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effect, "effect");
        if (effects.putIfAbsent(id, effect) != null) {
            throw new IllegalArgumentException("A circle effect " + id + " is already registered");
        }
        return effect;
    }

    public Optional<CircleEffect> get(Identifier id) {
        return Optional.ofNullable(effects.get(id));
    }

    public boolean contains(Identifier id) {
        return effects.containsKey(id);
    }

    public Set<Identifier> ids() {
        return Collections.unmodifiableSet(effects.keySet());
    }
}
