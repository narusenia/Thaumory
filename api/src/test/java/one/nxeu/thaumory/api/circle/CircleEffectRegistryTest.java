package one.nxeu.thaumory.api.circle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class CircleEffectRegistryTest {
    private static final Identifier LIGHT = Identifier.fromNamespaceAndPath("thaumory", "light");

    @Test
    void registersById() {
        CircleEffectRegistry registry = new CircleEffectRegistry();
        CircleEffect effect = context -> {};
        registry.register(LIGHT, effect);

        assertSame(effect, registry.get(LIGHT).orElseThrow());
        assertTrue(registry.contains(LIGHT));
        assertTrue(registry.get(Identifier.fromNamespaceAndPath("thaumory", "other")).isEmpty());
    }

    @Test
    void rejectsTheSameIdTwice() {
        CircleEffectRegistry registry = new CircleEffectRegistry();
        registry.register(LIGHT, context -> {});
        assertThrows(IllegalArgumentException.class, () -> registry.register(LIGHT, context -> {}));
    }
}
