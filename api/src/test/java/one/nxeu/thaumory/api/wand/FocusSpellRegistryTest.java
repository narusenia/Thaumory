package one.nxeu.thaumory.api.wand;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class FocusSpellRegistryTest {
    private static final Identifier LIGHT = Identifier.fromNamespaceAndPath("thaumory", "light");

    @Test
    void registersById() {
        FocusSpellRegistry registry = new FocusSpellRegistry();
        FocusSpell spell = context -> true;
        registry.register(LIGHT, spell);

        assertSame(spell, registry.get(LIGHT).orElseThrow());
        assertTrue(registry.contains(LIGHT));
        assertFalse(registry.contains(Identifier.fromNamespaceAndPath("thaumory", "fire")));
    }

    @Test
    void rejectsTheSameIdTwice() {
        FocusSpellRegistry registry = new FocusSpellRegistry();
        registry.register(LIGHT, context -> true);
        assertThrows(IllegalArgumentException.class, () -> registry.register(LIGHT, context -> false));
    }
}
