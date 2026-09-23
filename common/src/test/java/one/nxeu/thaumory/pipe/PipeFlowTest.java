package one.nxeu.thaumory.pipe;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.Test;

class PipeFlowTest {
    private static final PipeSettings SETTINGS = new PipeSettings(10, 4, 4);

    /** Takes {@code accepts} (all when empty) up to {@code capacity} in total. */
    private static final class Box implements PipeEndpoint {
        final int priority;
        final int capacity;
        final Set<Aspect> accepts;
        AspectList held;

        Box(int priority, int capacity, Set<Aspect> accepts, AspectList held) {
            this.priority = priority;
            this.capacity = capacity;
            this.accepts = accepts;
            this.held = held;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public AspectList contents() {
            return held;
        }

        @Override
        public int space(Aspect aspect) {
            return accepts.isEmpty() || accepts.contains(aspect) ? Math.max(0, capacity - held.total()) : 0;
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

    private static Box jar(AspectList held) {
        return new Box(1, 64, Set.of(), held);
    }

    private static Box core(Aspect... accepts) {
        return new Box(3, 64, Set.of(accepts), AspectList.empty());
    }

    @Test
    void drawnEssentiaArrivesTheNextStep() {
        Box jar = jar(AspectList.of(IGNIS, 20));
        Box core = core(IGNIS);

        AspectList buffer = PipeFlow.step(AspectList.empty(), 8, List.of(jar, core), SETTINGS);
        assertEquals(AspectList.of(IGNIS, 4), buffer);
        assertEquals(AspectList.empty(), core.held);

        buffer = PipeFlow.step(buffer, 8, List.of(jar, core), SETTINGS);
        assertEquals(AspectList.of(IGNIS, 4), core.held);
        assertEquals(AspectList.of(IGNIS, 4), buffer);
        assertEquals(AspectList.of(IGNIS, 12), jar.held);
    }

    @Test
    void nothingFlowsBetweenEqualPriorities() {
        Box full = jar(AspectList.of(IGNIS, 20));
        Box empty = jar(AspectList.empty());

        assertEquals(AspectList.empty(), PipeFlow.step(AspectList.empty(), 8, List.of(full, empty), SETTINGS));
        assertEquals(AspectList.of(IGNIS, 20), full.held);
    }

    @Test
    void receiversOnlyDrawWhatTheyTake() {
        Box jar = jar(AspectList.of(new AspectStack(IGNIS, 20), new AspectStack(AQUA, 20)));
        Box core = core(AQUA);

        assertEquals(AspectList.of(AQUA, 4), PipeFlow.step(AspectList.empty(), 8, List.of(jar, core), SETTINGS));
    }

    @Test
    void theNetworkHoldsNoMoreThanItsCapacity() {
        Box jar = jar(AspectList.of(IGNIS, 20));
        Box core = core(IGNIS);

        assertEquals(AspectList.of(IGNIS, 2), PipeFlow.step(AspectList.empty(), 2, List.of(jar, core), SETTINGS));
        assertEquals(AspectList.of(IGNIS, 18), jar.held);
    }

    @Test
    void whatIsOnItsWayCountsTowardsTheRoom() {
        // Gives Ignis but takes only Aqua, so nothing in transit goes back into it.
        Box source = new Box(1, 64, Set.of(AQUA), AspectList.of(IGNIS, 20));
        Box core = new Box(3, 5, Set.of(IGNIS), AspectList.empty());

        // 4 of the 6 in transit arrive; the other 2 fill the Core's last room on their way, so none is drawn.
        AspectList buffer = PipeFlow.step(AspectList.of(IGNIS, 6), 8, List.of(source, core), SETTINGS);
        assertEquals(AspectList.of(IGNIS, 4), core.held);
        assertEquals(AspectList.of(IGNIS, 2), buffer);
        assertEquals(AspectList.of(IGNIS, 20), source.held);
    }

    @Test
    void whatNoReceiverOfHigherPriorityTakesGoesBackDown() {
        Box jar = jar(AspectList.empty());
        Box core = new Box(3, 0, Set.of(IGNIS), AspectList.empty());

        assertEquals(AspectList.empty(), PipeFlow.step(AspectList.of(IGNIS, 3), 8, List.of(jar, core), SETTINGS));
        assertEquals(AspectList.of(IGNIS, 3), jar.held);
    }

    @Test
    void theHighestPriorityReceiverIsServedFirst() {
        Box jar = jar(AspectList.empty());
        Box labeled = new Box(2, 64, Set.of(IGNIS), AspectList.empty());
        Box core = core(IGNIS);

        AspectList buffer = PipeFlow.step(AspectList.of(IGNIS, 6), 8, List.of(jar, labeled, core), SETTINGS);
        assertEquals(AspectList.of(IGNIS, 4), core.held);
        assertEquals(AspectList.empty(), jar.held);
        // The labeled jar got the other 2, then gave them straight back towards the Core.
        assertEquals(AspectList.empty(), labeled.held);
        assertEquals(AspectList.of(IGNIS, 2), buffer);
    }

    @Test
    void theLowestPriorityContainerGivesFirst() {
        Box jar = jar(AspectList.of(IGNIS, 20));
        Box labeled = new Box(2, 64, Set.of(IGNIS), AspectList.of(IGNIS, 20));
        Box core = core(IGNIS);

        PipeFlow.step(AspectList.empty(), 8, List.of(labeled, jar, core), SETTINGS);
        assertEquals(AspectList.of(IGNIS, 20), labeled.held);
        // The Core drew 4 and the labeled jar 4 more, both from the unlabeled jar.
        assertEquals(AspectList.of(IGNIS, 12), jar.held);
    }
}
