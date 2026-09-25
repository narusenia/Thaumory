package one.nxeu.thaumory.aspect;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ThaumoryAspectsTest {
    private AspectRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
    }

    @Test
    void registersSixPrimalsTwelveCompoundsAndTheThirdTier() {
        assertEquals(19, registry.all().size());
        assertEquals(List.of(IGNIS, AER, VITA, AQUA, TERRA, MORS), registry.primals());
    }

    /** Requirements §2.2: pollution, bound poison, and what it wears away. */
    @Test
    void sordesIsVenenumBoundByVinculum() {
        assertEquals(List.of(AQUA, MORS, TERRA, MORS), SORDES.primalBreakdown());
        for (Aspect worn : List.of(IGNIS, AER, VITA, LUX, ARCANUM, HERBA, BELLUM, BESTIA, TEMPESTAS, ORDO, METALLUM, CHAOS)) {
            assertTrue(registry.cancels(SORDES, worn), worn.toString());
        }
        for (Aspect kept : List.of(AQUA, TERRA, MORS, UMBRA, VINCULUM, VENENUM)) {
            assertFalse(registry.cancels(SORDES, kept), kept.toString());
        }
        // Arcanum + Bestia is not registered yet.
        assertEquals(Optional.empty(), registry.opposite(SORDES));
    }

    @Test
    void primalsFormThreeOpposingPairs() {
        assertOpposite(IGNIS, AQUA);
        assertOpposite(AER, TERRA);
        assertOpposite(VITA, MORS);
    }

    static List<Arguments> compounds() {
        return List.of(
                Arguments.of(LUX, IGNIS, AER, UMBRA),
                Arguments.of(ARCANUM, AER, VITA, VINCULUM),
                Arguments.of(HERBA, VITA, AQUA, BELLUM),
                Arguments.of(UMBRA, AQUA, TERRA, LUX),
                Arguments.of(VINCULUM, TERRA, MORS, ARCANUM),
                Arguments.of(BELLUM, MORS, IGNIS, HERBA),
                Arguments.of(BESTIA, IGNIS, VITA, VENENUM),
                Arguments.of(TEMPESTAS, AER, AQUA, METALLUM),
                Arguments.of(ORDO, VITA, TERRA, CHAOS),
                Arguments.of(VENENUM, AQUA, MORS, BESTIA),
                Arguments.of(METALLUM, TERRA, IGNIS, TEMPESTAS),
                Arguments.of(CHAOS, MORS, AER, ORDO));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("compounds")
    void compoundMatchesRequirementsTable(Aspect compound, Aspect first, Aspect second, Aspect opposite) {
        assertEquals(Set.of(first, second), Set.copyOf(compound.components()));
        assertOpposite(compound, opposite);
        assertTrue(compound.vector().add(opposite.vector()).isCloseTo(AspectVector.ZERO, 1e-9));
    }

    @Test
    void adjacentCompoundsSitBetweenTheirPrimals() {
        for (Aspect compound : List.of(LUX, ARCANUM, HERBA, UMBRA, VINCULUM, BELLUM)) {
            assertEquals(Math.sqrt(3), compound.vector().length(), 1e-9, compound.toString());
        }
    }

    /** Requirements §9: these land exactly where the primal between them points. */
    @Test
    void skipOneCompoundsShareTheirMiddlePrimalsVector() {
        assertTrue(BESTIA.vector().isCloseTo(AER.vector(), 1e-9));
        assertTrue(TEMPESTAS.vector().isCloseTo(VITA.vector(), 1e-9));
        assertTrue(ORDO.vector().isCloseTo(AQUA.vector(), 1e-9));
        assertTrue(VENENUM.vector().isCloseTo(TERRA.vector(), 1e-9));
        assertTrue(METALLUM.vector().isCloseTo(MORS.vector(), 1e-9));
        assertTrue(CHAOS.vector().isCloseTo(IGNIS.vector(), 1e-9));
    }

    @Test
    void cancellationFollowsPrimalBreakdown() {
        assertTrue(registry.cancels(IGNIS, AQUA));
        assertTrue(registry.cancels(LUX, AQUA)); // Lux carries Ignis
        assertTrue(registry.cancels(HERBA, BELLUM)); // Vita/Mors and Aqua/Ignis
        assertTrue(registry.cancels(ARCANUM, TERRA)); // Aer/Terra
        assertFalse(registry.cancels(IGNIS, AER));
        assertFalse(registry.cancels(LUX, ARCANUM)); // Ignis, Aer, Vita never oppose
    }

    @Test
    void usesThaumoryNamespaceAndLatinNames() {
        assertEquals("thaumory:arcanum", ARCANUM.id().toString());
        assertEquals("aspect.thaumory.arcanum", ARCANUM.translationKey());
    }

    private void assertOpposite(Aspect a, Aspect b) {
        assertEquals(Optional.of(b), registry.opposite(a), a + " ↔ " + b);
        assertEquals(Optional.of(a), registry.opposite(b), b + " ↔ " + a);
    }
}
