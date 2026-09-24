package one.nxeu.thaumory.infusion;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import org.junit.jupiter.api.Test;

class ItemEssentiaTest {
    private static final Set<net.minecraft.resources.Identifier> TELEPORT = Set.of(ARCANUM.id(), AER.id());

    @Test
    void fillTakesOnlyTheStoredAspectsUpToTheCapacity() {
        AspectList stored = AspectList.of(ARCANUM, 60);
        AspectList offered = AspectList.of(new AspectStack(ARCANUM, 10), new AspectStack(AER, 10), new AspectStack(TERRA, 10));

        ItemEssentia.Fill fill = ItemEssentia.fill(stored, offered, TELEPORT, 64);

        assertEquals(AspectList.of(new AspectStack(ARCANUM, 4), new AspectStack(AER, 10)), fill.taken());
        assertEquals(AspectList.of(new AspectStack(ARCANUM, 64), new AspectStack(AER, 10)), fill.stored());
    }

    @Test
    void aFullItemTakesNothing() {
        AspectList stored = AspectList.of(new AspectStack(ARCANUM, 64), new AspectStack(AER, 64));

        assertTrue(ItemEssentia.fill(stored, AspectList.of(AER, 5), TELEPORT, 64).taken().isEmpty());
    }

    @Test
    void payTakesEveryAspectOrNothing() {
        AspectRegistry registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
        AspectList stored = AspectList.of(new AspectStack(ARCANUM, 6), new AspectStack(AER, 3));
        Map<net.minecraft.resources.Identifier, Integer> cost = Map.of(ARCANUM.id(), 4, AER.id(), 4);

        assertEquals(Optional.empty(), ItemEssentia.pay(stored, cost, registry));
        assertEquals(Optional.of(AspectList.of(new AspectStack(ARCANUM, 2), new AspectStack(AER, 1))),
                ItemEssentia.pay(AspectList.of(new AspectStack(ARCANUM, 6), new AspectStack(AER, 5)), cost, registry));
    }
}
