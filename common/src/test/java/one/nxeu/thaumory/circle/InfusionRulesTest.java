package one.nxeu.thaumory.circle;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import org.junit.jupiter.api.Test;

class InfusionRulesTest {
    private static final InfusionSettings SETTINGS = InfusionSettings.DEFAULT;

    @Test
    void anActiveEffectsUseCostScalesLikeATriggeredCircle() {
        assertEquals(Map.of(ARCANUM.id(), 4, AER.id(), 4), InfusionRules.useCost(4, 1.0, ARCANUM, AER));
        assertEquals(Map.of(ARCANUM.id(), 3, AER.id(), 3), InfusionRules.useCost(4, 0.75, ARCANUM, AER));
        assertEquals(Map.of(ARCANUM.id(), 1, AER.id(), 1), InfusionRules.useCost(1, 0.25, ARCANUM, AER));
    }

    @Test
    void costsEachRuneAndSlotThreeScaled() {
        assertEquals(AspectList.of(new AspectStack(ARCANUM, 32), new AspectStack(AER, 32), new AspectStack(TERRA, 4)),
                InfusionRules.cost(InfusionCost.DEFAULT, 1, ARCANUM, AER, Optional.of(TERRA)));
        // One amplifier: 1.5 times.
        assertEquals(AspectList.of(new AspectStack(ARCANUM, 48), new AspectStack(AER, 48), new AspectStack(TERRA, 6)),
                InfusionRules.cost(InfusionCost.DEFAULT, 1.5, ARCANUM, AER, Optional.of(TERRA)));
        assertEquals(AspectList.of(new AspectStack(ARCANUM, 1), new AspectStack(AER, 1)),
                InfusionRules.cost(new InfusionCost(1, 1), 0.25, ARCANUM, AER, Optional.empty()));
    }

    @Test
    void eachHalfOfStrengthIsALevel() {
        assertEquals(1, InfusionRules.level(1, SETTINGS));
        assertEquals(2, InfusionRules.level(1.5, SETTINGS));
        assertEquals(3, InfusionRules.level(2, SETTINGS));
        assertEquals(1, InfusionRules.level(0.75, SETTINGS));
        assertEquals(5, InfusionRules.level(9, SETTINGS));
    }

    @Test
    void instabilityOverTheThresholdRaisesTheChanceOfFailure() {
        assertEquals(0.1, InfusionRules.failureChance(0, 3, SETTINGS), 1e-9);
        assertEquals(0.1, InfusionRules.failureChance(3, 3, SETTINGS), 1e-9);
        assertEquals(0.5, InfusionRules.failureChance(5, 3, SETTINGS), 1e-9);
        assertEquals(1, InfusionRules.failureChance(20, 3, SETTINGS), 1e-9);
    }

    @Test
    void halfOfWhatIsLostBecomesFluxRoundedUp() {
        assertEquals(34, InfusionRules.failureFlux(AspectList.of(new AspectStack(ARCANUM, 32), new AspectStack(AER, 32), new AspectStack(TERRA, 4)), SETTINGS));
        assertEquals(2, InfusionRules.failureFlux(AspectList.of(ARCANUM, 3), SETTINGS));
    }
}
