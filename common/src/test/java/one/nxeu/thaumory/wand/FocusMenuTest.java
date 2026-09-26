package one.nxeu.thaumory.wand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class FocusMenuTest {
    private static final Identifier LIGHT = Identifier.fromNamespaceAndPath("thaumory", "light_focus");
    private static final Identifier FIRE = Identifier.fromNamespaceAndPath("thaumory", "fire_focus");
    private static final Identifier DIRT = Identifier.withDefaultNamespace("dirt");
    private static final Set<Identifier> FOCI = Set.of(LIGHT, FIRE);

    @Test
    void offersTheFirstFocusOfEachKindInSlotOrder() {
        List<Identifier> items = Arrays.asList(DIRT, FIRE, null, LIGHT, FIRE, LIGHT);
        assertEquals(List.of(1, 3), FocusMenu.slots(items, FOCI, Optional.empty()));
    }

    @Test
    void leavesOutTheKindTheWandCarries() {
        List<Identifier> items = Arrays.asList(LIGHT, FIRE, LIGHT);
        assertEquals(List.of(1), FocusMenu.slots(items, FOCI, Optional.of(LIGHT)));
    }

    @Test
    void picksNothingNearTheCentre() {
        assertTrue(FocusMenu.pick(4, 3, 4, 10).isEmpty());
        assertTrue(FocusMenu.pick(0, 50, 0, 10).isEmpty());
    }

    @Test
    void picksClockwiseFromTheTop() {
        assertEquals(OptionalInt.of(0), FocusMenu.pick(4, 0, -40, 10));
        assertEquals(OptionalInt.of(1), FocusMenu.pick(4, 40, 0, 10));
        assertEquals(OptionalInt.of(2), FocusMenu.pick(4, 0, 40, 10));
        assertEquals(OptionalInt.of(3), FocusMenu.pick(4, -40, 0, 10));
    }

    @Test
    void eachEntryOwnsTheSectorAroundIt() {
        // Just left of the top still belongs to the first entry, not the last.
        assertEquals(OptionalInt.of(0), FocusMenu.pick(4, -10, -40, 5));
        assertEquals(OptionalInt.of(0), FocusMenu.pick(1, 0, 40, 5));
        assertEquals(OptionalInt.of(2), FocusMenu.pick(3, -40, 10, 5));
    }

    @Test
    void anglesGoClockwiseFromTheTop() {
        assertEquals(0, FocusMenu.angle(0, 4), 1e-9);
        assertEquals(Math.PI / 2, FocusMenu.angle(1, 4), 1e-9);
    }
}
