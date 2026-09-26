package one.nxeu.thaumory.wand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.wand.WandDisplay.Bar;
import org.junit.jupiter.api.Test;

class WandDisplayTest {
    private static final Identifier AER = Identifier.fromNamespaceAndPath("thaumory", "aer");
    private static final Identifier ARCANUM = Identifier.fromNamespaceAndPath("thaumory", "arcanum");
    private static final Identifier IGNIS = Identifier.fromNamespaceAndPath("thaumory", "ignis");
    private static final Identifier LUX = Identifier.fromNamespaceAndPath("thaumory", "lux");

    @Test
    void theFocusAspectsComeFirstEvenWhenEmpty() {
        List<Bar> bars = WandDisplay.bars(Map.of(LUX, 1, ARCANUM, 2), Map.of(IGNIS, 5, AER, 3, ARCANUM, 4));
        assertEquals(List.of(
                new Bar(ARCANUM, 4, true, false),
                new Bar(LUX, 0, true, true),
                new Bar(AER, 3, false, false),
                new Bar(IGNIS, 5, false, false)), bars);
    }

    @Test
    void aFocusAspectBelowOneCastIsShort() {
        assertEquals(List.of(new Bar(LUX, 1, true, true)), WandDisplay.bars(Map.of(LUX, 2), Map.of(LUX, 1)));
        assertEquals(List.of(new Bar(LUX, 2, true, false)), WandDisplay.bars(Map.of(LUX, 2), Map.of(LUX, 2)));
    }

    @Test
    void withoutAFocusOnlyWhatItHoldsShows() {
        assertEquals(List.of(), WandDisplay.bars(Map.of(), Map.of()));
        assertEquals(List.of(new Bar(IGNIS, 5, false, false)), WandDisplay.bars(Map.of(), Map.of(IGNIS, 5)));
    }
}
