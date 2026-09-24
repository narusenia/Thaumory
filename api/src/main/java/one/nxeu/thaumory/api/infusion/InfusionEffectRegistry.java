package one.nxeu.thaumory.api.infusion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.Experimental;

/** Every infusion effect by the id of the circle effect it belongs to. Register during mod initialization. */
@Experimental
public final class InfusionEffectRegistry {
    private final Map<Identifier, InfusionEffect> effects = new LinkedHashMap<>();

    public InfusionEffect register(Identifier id, InfusionEffect effect) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(effect, "effect");
        if (effects.putIfAbsent(id, effect) != null) {
            throw new IllegalArgumentException("An infusion effect " + id + " is already registered");
        }
        return effect;
    }

    public Optional<InfusionEffect> get(Identifier id) {
        return Optional.ofNullable(effects.get(id));
    }

    public boolean contains(Identifier id) {
        return effects.containsKey(id);
    }

    public Set<Identifier> ids() {
        return Collections.unmodifiableSet(effects.keySet());
    }
}
