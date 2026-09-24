package one.nxeu.thaumory.api.infusion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class InfusionEffectRegistryTest {
    private static final Identifier HEALING = Identifier.fromNamespaceAndPath("thaumory", "healing");

    @Test
    void registersByIdAndIsPassiveByDefault() {
        InfusionEffectRegistry registry = new InfusionEffectRegistry();
        InfusionEffect effect = new InfusionEffect() {};
        registry.register(HEALING, effect);

        assertSame(effect, registry.get(HEALING).orElseThrow());
        assertTrue(registry.contains(HEALING));
        assertFalse(effect.active());
    }

    @Test
    void rejectsTheSameIdTwice() {
        InfusionEffectRegistry registry = new InfusionEffectRegistry();
        registry.register(HEALING, new InfusionEffect() {});
        assertThrows(IllegalArgumentException.class, () -> registry.register(HEALING, new InfusionEffect() {}));
    }
}
