package one.nxeu.thaumory.jar;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.Optional;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EssentiaTransferTest {
    private static final int CAPACITY = 64;
    private static final AspectList CRUCIBLE = AspectList.of(new AspectStack(HERBA, 100), new AspectStack(TERRA, 30), new AspectStack(IGNIS, 20));

    private AspectRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
    }

    @Test
    void unlabeledJarDrawsTheLargestAspectUpToCapacity() {
        EssentiaTransfer.Result result = EssentiaTransfer.draw(CRUCIBLE, AspectList.empty(), Optional.empty(), CAPACITY, registry);

        assertEquals(AspectList.of(HERBA, 64), result.to());
        assertEquals(AspectList.of(new AspectStack(HERBA, 36), new AspectStack(TERRA, 30), new AspectStack(IGNIS, 20)), result.from());
        assertEquals(0, result.flux());
    }

    @Test
    void labeledJarDrawsOnlyItsAspect() {
        EssentiaTransfer.Result result = EssentiaTransfer.draw(CRUCIBLE, AspectList.of(TERRA, 50), Optional.of(TERRA), CAPACITY, registry);

        assertEquals(AspectList.of(TERRA, 64), result.to());
        assertEquals(AspectList.of(new AspectStack(HERBA, 100), new AspectStack(TERRA, 16), new AspectStack(IGNIS, 20)), result.from());
    }

    @Test
    void labeledJarDrawsNothingWhenItsAspectIsMissing() {
        EssentiaTransfer.Result result = EssentiaTransfer.draw(CRUCIBLE, AspectList.empty(), Optional.of(AQUA), CAPACITY, registry);

        assertEquals(CRUCIBLE, result.from());
        assertTrue(result.to().isEmpty());
    }

    @Test
    void fullJarDrawsNothing() {
        EssentiaTransfer.Result result = EssentiaTransfer.draw(CRUCIBLE, AspectList.of(TERRA, 64), Optional.empty(), CAPACITY, registry);
        assertEquals(CRUCIBLE, result.from());
    }

    @Test
    void mixingOppositesInAnUnlabeledJarCancelsAtOnce() {
        // The jar holds Aqua 10 and draws the largest aspect of a source holding Ignis 25.
        EssentiaTransfer.Result result = EssentiaTransfer.draw(AspectList.of(IGNIS, 25), AspectList.of(AQUA, 10), Optional.empty(), CAPACITY, registry);

        assertEquals(AspectList.of(IGNIS, 15), result.to());
        assertEquals(20, result.flux());
        assertTrue(result.from().isEmpty());
    }

    @Test
    void pouringIntoACrucibleSpillsWhatDoesNotFit() {
        AspectList jar = AspectList.of(new AspectStack(HERBA, 40), new AspectStack(TERRA, 20));
        EssentiaTransfer.Result result = EssentiaTransfer.pour(jar, AspectList.of(AQUA, 450), Optional.empty(), 500, true, false, registry);

        assertEquals(AspectList.of(new AspectStack(AQUA, 450), new AspectStack(HERBA, 40), new AspectStack(TERRA, 10)), result.to());
        assertTrue(result.from().isEmpty());
        assertEquals(10, result.flux());
    }

    @Test
    void pouringIntoAJarKeepsWhatDoesNotFit() {
        AspectList jar = AspectList.of(HERBA, 40);
        EssentiaTransfer.Result result = EssentiaTransfer.pour(jar, AspectList.of(HERBA, 50), Optional.empty(), CAPACITY, false, true, registry);

        assertEquals(AspectList.of(HERBA, 64), result.to());
        assertEquals(AspectList.of(HERBA, 26), result.from());
        assertEquals(0, result.flux());
    }

    @Test
    void pouringIntoALabeledJarMovesOnlyItsAspect() {
        AspectList jar = AspectList.of(new AspectStack(HERBA, 10), new AspectStack(TERRA, 5));
        EssentiaTransfer.Result result = EssentiaTransfer.pour(jar, AspectList.empty(), Optional.of(TERRA), CAPACITY, false, true, registry);

        assertEquals(AspectList.of(TERRA, 5), result.to());
        assertEquals(AspectList.of(HERBA, 10), result.from());
    }

    @Test
    void pouringOppositesIntoAnUnlabeledJarCancels() {
        EssentiaTransfer.Result result = EssentiaTransfer.pour(AspectList.of(IGNIS, 8), AspectList.of(AQUA, 5), Optional.empty(),
                CAPACITY, false, true, registry);

        assertEquals(AspectList.of(IGNIS, 3), result.to());
        assertEquals(10, result.flux());
    }

    @Test
    void labelsOnlyAJarHoldingOneAspect() {
        assertEquals(Optional.of(new JarContents(AspectList.of(HERBA, 4), Optional.of(HERBA))),
                new JarContents(AspectList.of(HERBA, 4), Optional.empty()).labeled());
        assertEquals(Optional.empty(), JarContents.EMPTY.labeled());
        assertEquals(Optional.empty(), new JarContents(CRUCIBLE, Optional.empty()).labeled());
    }

    @Test
    void jarContentsRoundTrip() {
        JarContents contents = new JarContents(AspectList.of(HERBA, 4), Optional.of(HERBA));
        var json = JarContents.codec(registry).encodeStart(JsonOps.INSTANCE, contents).getOrThrow();
        assertEquals(contents, JarContents.codec(registry).parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
