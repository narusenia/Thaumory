package one.nxeu.thaumory.api.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class AspectListTest {
    private static final Aspect FIRE = Aspect.primal(id("fire"), 0, 0);
    private static final Aspect WIND = Aspect.primal(id("wind"), 0, 90);
    private static final Aspect WATER = Aspect.primal(id("water"), 0, 180);
    private static final Aspect EARTH = Aspect.primal(id("earth"), 0, 270);

    @Test
    void mergesAmountsAndIgnoresNonPositive() {
        AspectList list = AspectList.builder()
                .add(FIRE, 2)
                .add(WIND, 0)
                .add(FIRE, 3)
                .add(WATER, -1)
                .build();

        assertEquals(5, list.amount(FIRE));
        assertFalse(list.contains(WIND));
        assertEquals(1, list.size());
        assertEquals(5, list.total());
    }

    @Test
    void emptyListIsShared() {
        assertSame(AspectList.empty(), AspectList.builder().add(FIRE, 0).build());
        assertTrue(AspectList.empty().isEmpty());
    }

    @Test
    void plusAndMinus() {
        AspectList a = AspectList.of(new AspectStack(FIRE, 4), new AspectStack(WIND, 1));
        AspectList b = AspectList.of(new AspectStack(FIRE, 1), new AspectStack(WATER, 2));

        assertEquals(AspectList.of(new AspectStack(FIRE, 5), new AspectStack(WIND, 1), new AspectStack(WATER, 2)),
                a.plus(b));
        assertEquals(AspectList.of(new AspectStack(FIRE, 3), new AspectStack(WIND, 1)), a.minus(b));
    }

    @Test
    void minusDropsAspectsThatRunOut() {
        AspectList a = AspectList.of(FIRE, 2);
        assertEquals(AspectList.empty(), a.minus(AspectList.of(FIRE, 5)));
    }

    @Test
    void containsAll() {
        AspectList stock = AspectList.of(new AspectStack(FIRE, 5), new AspectStack(WIND, 2));
        assertTrue(stock.containsAll(AspectList.of(new AspectStack(FIRE, 5), new AspectStack(WIND, 1))));
        assertFalse(stock.containsAll(AspectList.of(FIRE, 6)));
        assertFalse(stock.containsAll(AspectList.of(WATER, 1)));
        assertTrue(stock.containsAll(AspectList.empty()));
    }

    @Test
    void scaleRoundsDownExactly() {
        // 4 * 3/4 = 3, 3 * 3/4 = 2.25 -> 2, 1 * 3/4 = 0.75 -> dropped.
        AspectList list = AspectList.of(new AspectStack(FIRE, 4), new AspectStack(WIND, 3), new AspectStack(WATER, 1));
        assertEquals(AspectList.of(new AspectStack(FIRE, 3), new AspectStack(WIND, 2)), list.scale(3, 4));
    }

    @Test
    void scaleByThirdDoesNotLoseToFloatingPoint() {
        // 3 * (1/3) in doubles is 0.999..., which would floor to 0.
        assertEquals(AspectList.of(FIRE, 1), AspectList.of(FIRE, 3).scale(1, 3));
    }

    @Test
    void scaleRejectsInvalidRatio() {
        assertThrows(IllegalArgumentException.class, () -> AspectList.of(FIRE, 1).scale(1, 0));
        assertThrows(IllegalArgumentException.class, () -> AspectList.of(FIRE, 1).scale(-1, 2));
    }

    @Test
    void sortedByAmountBreaksTiesById() {
        AspectList list = AspectList.of(
                new AspectStack(WIND, 2), new AspectStack(FIRE, 5), new AspectStack(EARTH, 2));

        assertEquals(List.of(new AspectStack(FIRE, 5), new AspectStack(EARTH, 2), new AspectStack(WIND, 2)),
                list.sortedByAmount());
        assertEquals(Optional.of(new AspectStack(FIRE, 5)), list.largest());
        assertEquals(Optional.empty(), AspectList.empty().largest());
    }

    @Test
    void limitKeepsLargestInOriginalOrder() {
        AspectList list = AspectList.of(
                new AspectStack(WIND, 1), new AspectStack(FIRE, 5), new AspectStack(WATER, 3), new AspectStack(EARTH, 2));

        AspectList limited = list.limit(2);
        assertEquals(List.of(new AspectStack(FIRE, 5), new AspectStack(WATER, 3)), limited.stacks());
        assertSame(list, list.limit(4));
    }

    @Test
    void findsCancellingPairsThroughCompounds() {
        AspectRegistry registry = new AspectRegistry();
        List.of(FIRE, WIND, WATER, EARTH).forEach(registry::register);
        Aspect smoke = registry.register(Aspect.compound(id("smoke"), 0, FIRE, WIND));

        AspectList list = AspectList.of(
                new AspectStack(smoke, 2), new AspectStack(WATER, 1), new AspectStack(WIND, 1));

        assertEquals(List.of(List.of(smoke, WATER)), list.cancellingPairs(registry));
        assertEquals(List.of(), AspectList.of(new AspectStack(FIRE, 1), new AspectStack(WIND, 1))
                .cancellingPairs(registry));
    }

    @Test
    void stackRejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> new AspectStack(FIRE, 0));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }
}
