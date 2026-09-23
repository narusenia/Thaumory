package one.nxeu.thaumory.pipe;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.Test;

class RestrictedEndpointTest {
    /** Takes and gives anything, up to 64 in total. */
    private static final class Open implements PipeEndpoint {
        AspectList held;

        Open(AspectList held) {
            this.held = held;
        }

        @Override
        public int priority() {
            return 1;
        }

        @Override
        public AspectList contents() {
            return held;
        }

        @Override
        public int space(Aspect aspect) {
            return 64 - held.total();
        }

        @Override
        public int insert(Aspect aspect, int max) {
            int amount = Math.min(max, space(aspect));
            held = held.plus(AspectList.of(aspect, amount));
            return amount;
        }

        @Override
        public int extract(Aspect aspect, int max) {
            int amount = Math.min(max, held.amount(aspect));
            held = held.minus(AspectList.of(aspect, amount));
            return amount;
        }
    }

    @Test
    void aPlainPipeAmongFiltersLetsAnythingThrough() {
        assertEquals(Optional.empty(), RestrictedEndpoint.union(List.of(Optional.of(IGNIS), Optional.empty())));
        assertEquals(Optional.of(Set.of(IGNIS, AQUA)), RestrictedEndpoint.union(List.of(Optional.of(IGNIS), Optional.of(AQUA))));
    }

    @Test
    void aFilterPassesOnlyItsAspects() {
        Open jar = new Open(AspectList.of(new AspectStack(IGNIS, 10), new AspectStack(TERRA, 5)));
        RestrictedEndpoint filtered = new RestrictedEndpoint(jar, Optional.of(Set.of(IGNIS)), false);

        assertEquals(AspectList.of(IGNIS, 10), filtered.contents());
        assertEquals(0, filtered.extract(TERRA, 5));
        assertEquals(0, filtered.space(AQUA));
        assertEquals(0, filtered.insert(AQUA, 5));
        assertEquals(4, filtered.extract(IGNIS, 4));
        assertEquals(3, filtered.insert(IGNIS, 3));
    }

    @Test
    void aPumpOnlyDraws() {
        Open crucible = new Open(AspectList.of(IGNIS, 10));
        RestrictedEndpoint pumped = new RestrictedEndpoint(crucible, Optional.empty(), true);

        assertEquals(0, pumped.space(IGNIS));
        assertEquals(0, pumped.insert(IGNIS, 3));
        assertEquals(4, pumped.extract(IGNIS, 4));
    }
}
