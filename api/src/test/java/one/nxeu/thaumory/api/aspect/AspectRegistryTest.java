package one.nxeu.thaumory.api.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.text.TextEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AspectRegistryTest {
    private AspectRegistry registry;
    private Aspect fire;
    private Aspect wind;
    private Aspect water;
    private Aspect earth;

    @BeforeEach
    void setUp() {
        registry = new AspectRegistry();
        fire = registry.register(Aspect.primal(id("fire"), 0xFF0000, 0));
        wind = registry.register(Aspect.primal(id("wind"), 0xFFFF00, 90));
        water = registry.register(Aspect.primal(id("water"), 0x0000FF, 180));
        earth = registry.register(Aspect.primal(id("earth"), 0x884400, 270));
    }

    @Test
    void nameEffectIsOptionalAndKeepsTheRest() {
        assertEquals(Optional.empty(), fire.nameEffect());
        Aspect flickering = fire.withNameEffect(TextEffect.FLICKER);
        assertEquals(Optional.of(TextEffect.FLICKER), flickering.nameEffect());
        assertEquals(fire, flickering);
        assertEquals(fire.color(), flickering.color());
        assertEquals(fire.degrees(), flickering.degrees());
    }

    @Test
    void rejectsDuplicateId() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(Aspect.primal(id("fire"), 0, 45)));
    }

    @Test
    void rejectsPrimalPointingTheSameWay() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(Aspect.primal(id("heat"), 0, 360)));
    }

    @Test
    void rejectsCompoundOfUnregisteredAspect() {
        Aspect stray = Aspect.primal(id("stray"), 0, 45);
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(Aspect.compound(id("mix"), 0, fire, stray)));
    }

    @Test
    void rejectsCompoundOfOpposites() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(Aspect.compound(id("steam"), 0, fire, water)));
    }

    @Test
    void rejectsCompoundOfSameAspectTwice() {
        assertThrows(IllegalArgumentException.class, () -> Aspect.compound(id("inferno"), 0, fire, fire));
    }

    @Test
    void rejectsSecondCompoundWithSameComponents() {
        registry.register(Aspect.compound(id("smoke"), 0, fire, wind));
        assertThrows(IllegalArgumentException.class,
                () -> registry.register(Aspect.compound(id("ash"), 0, wind, fire)));
    }

    @Test
    void rejectsRegistrationWhenFrozen() {
        registry.freeze();
        assertTrue(registry.isFrozen());
        assertThrows(IllegalStateException.class,
                () -> registry.register(Aspect.primal(id("late"), 0, 45)));
    }

    @Test
    void primalOppositeIsTheOtherDirection() {
        assertEquals(Optional.of(water), registry.opposite(fire));
        assertEquals(Optional.of(earth), registry.opposite(wind));
        assertTrue(registry.areOpposite(water, fire));
        assertFalse(registry.areOpposite(fire, wind));
    }

    @Test
    void compoundOppositeIsMadeOfOppositeComponents() {
        Aspect smoke = registry.register(Aspect.compound(id("smoke"), 0, fire, wind));
        assertEquals(Optional.empty(), registry.opposite(smoke));

        Aspect mud = registry.register(Aspect.compound(id("mud"), 0, earth, water));
        assertEquals(Optional.of(mud), registry.opposite(smoke));
        assertEquals(Optional.of(smoke), registry.opposite(mud));
    }

    @Test
    void compoundBreaksDownIntoPrimalsInOrder() {
        Aspect smoke = registry.register(Aspect.compound(id("smoke"), 0, fire, wind));
        Aspect soot = registry.register(Aspect.compound(id("soot"), 0, smoke, earth));

        assertEquals(List.of(fire, wind, earth), soot.primalBreakdown());
        assertTrue(soot.vector().isCloseTo(new AspectVector(1, 0), 1e-9));
    }

    @Test
    void keepsRegistrationOrder() {
        assertEquals(List.of(fire, wind, water, earth), List.copyOf(registry.all()));
        assertEquals(List.of(fire, wind, water, earth), registry.primals());
    }

    @Test
    void compoundHasNoDirection() {
        Aspect smoke = registry.register(Aspect.compound(id("smoke"), 0, fire, wind));
        assertThrows(IllegalStateException.class, smoke::degrees);
    }

    @Test
    void normalizesPrimalDirection() {
        assertEquals(270, Aspect.primal(id("south"), 0, -90).degrees(), 1e-9);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
