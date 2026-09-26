package one.nxeu.thaumory.pouch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import org.junit.jupiter.api.Test;

class PouchRefillTest {
    private static final Aspect IGNIS = Aspect.primal(Identifier.parse("test:ignis"), 0xFF0000, 0);
    private static final Aspect AQUA = Aspect.primal(Identifier.parse("test:aqua"), 0x0000FF, 180);
    private static final Aspect AER = Aspect.primal(Identifier.parse("test:aer"), 0xFFFF00, 60);

    @Test
    void anItemWantsWhatItAcceptsUpToTheRateAndItsRoom() {
        AspectList stored = AspectList.builder().add(IGNIS, 62).add(AQUA, 10).build();
        AspectList wanted = PouchRefill.wanted(stored, Set.of(IGNIS, AQUA), 64, 4);
        assertEquals(AspectList.builder().add(IGNIS, 2).add(AQUA, 4).build(), wanted);
    }

    @Test
    void aFullItemWantsNothing() {
        assertEquals(AspectList.empty(), PouchRefill.wanted(AspectList.of(IGNIS, 70), Set.of(IGNIS), 64, 4));
    }

    @Test
    void theFirstJarIsDrawnFirstAndTheNextMakesUpTheRest() {
        List<AspectList> jars = List.of(AspectList.of(IGNIS, 3), AspectList.of(AQUA, 10), AspectList.of(IGNIS, 10));
        PouchRefill.Draw draw = PouchRefill.draw(jars, AspectList.builder().add(IGNIS, 4).add(AER, 4).build());

        assertEquals(AspectList.of(IGNIS, 4), draw.taken());
        assertEquals(List.of(AspectList.empty(), AspectList.of(AQUA, 10), AspectList.of(IGNIS, 9)), draw.jars());
    }
}
