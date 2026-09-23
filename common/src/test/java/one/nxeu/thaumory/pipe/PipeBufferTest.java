package one.nxeu.thaumory.pipe;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.Test;

class PipeBufferTest {
    @Test
    void splitsEvenlyWithTheRestUpFront() {
        AspectList buffer = AspectList.of(new AspectStack(IGNIS, 7), new AspectStack(AQUA, 2));

        List<AspectList> shares = PipeBuffer.split(buffer, 3);
        assertEquals(List.of(
                AspectList.of(new AspectStack(IGNIS, 3), new AspectStack(AQUA, 1)),
                AspectList.of(new AspectStack(IGNIS, 2), new AspectStack(AQUA, 1)),
                AspectList.of(IGNIS, 2)), shares);
        assertEquals(buffer, shares.stream().reduce(AspectList.empty(), AspectList::plus));
    }

    @Test
    void emptyBufferGivesEmptyShares() {
        assertEquals(List.of(AspectList.empty(), AspectList.empty()), PipeBuffer.split(AspectList.empty(), 2));
    }
}
