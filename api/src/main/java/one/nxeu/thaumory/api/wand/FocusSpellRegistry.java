package one.nxeu.thaumory.api.wand;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.Experimental;

/** Every focus spell by id. Register during mod initialization. */
@Experimental
public final class FocusSpellRegistry {
    private final Map<Identifier, FocusSpell> spells = new LinkedHashMap<>();

    public FocusSpell register(Identifier id, FocusSpell spell) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spell, "spell");
        if (spells.putIfAbsent(id, spell) != null) {
            throw new IllegalArgumentException("A focus spell " + id + " is already registered");
        }
        return spell;
    }

    public Optional<FocusSpell> get(Identifier id) {
        return Optional.ofNullable(spells.get(id));
    }

    public boolean contains(Identifier id) {
        return spells.containsKey(id);
    }

    public Set<Identifier> ids() {
        return Collections.unmodifiableSet(spells.keySet());
    }
}
