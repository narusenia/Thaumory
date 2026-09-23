package one.nxeu.thaumory.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UnknownGlyphsTest {
    private static final List<String> ASPECTS = List.of("ignis", "aer", "vita", "aqua", "terra", "mors", "lux", "arcanum", "herba",
            "umbra", "vinculum", "bellum", "bestia", "tempestas", "ordo", "venenum", "metallum", "chaos");

    @Test
    void sameAspectAlwaysShowsTheSameGlyphs() {
        assertEquals(UnknownGlyphs.of("thaumory:ignis"), UnknownGlyphs.of("thaumory:ignis"));
    }

    @Test
    void threeOrFourGlyphsFromThePrivateUseRange() {
        for (String aspect : ASPECTS) {
            String glyphs = UnknownGlyphs.of("thaumory:" + aspect);
            assertTrue(glyphs.length() == 3 || glyphs.length() == 4, glyphs);
            glyphs.codePoints().forEach(c -> assertTrue(c >= UnknownGlyphs.FIRST && c < UnknownGlyphs.FIRST + UnknownGlyphs.COUNT));
        }
    }

    @Test
    void everyBuiltInAspectLooksDifferent() {
        Set<String> seen = new HashSet<>();
        for (String aspect : ASPECTS) {
            seen.add(UnknownGlyphs.of("thaumory:" + aspect));
        }
        assertEquals(ASPECTS.size(), seen.size());
    }
}
