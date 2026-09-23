package one.nxeu.thaumory.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.Test;

class AspectColorsTest {
    private static final Aspect RED = Aspect.primal(Identifier.parse("test:red"), 0xFF0000, 0);
    private static final Aspect BLUE = Aspect.primal(Identifier.parse("test:blue"), 0x0000FF, 180);

    @Test
    void singleAspectKeepsItsColor() {
        assertEquals(0xFF0000, AspectColors.mix(AspectList.of(RED, 7)));
    }

    @Test
    void mixesByAmount() {
        // Three parts red to one part blue.
        assertEquals(0xBF0040, AspectColors.mix(AspectList.of(new AspectStack(RED, 30), new AspectStack(BLUE, 10))));
    }

    @Test
    void emptyIsWhite() {
        assertEquals(0xFFFFFF, AspectColors.mix(AspectList.empty()));
    }
}
