package one.nxeu.thaumory.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.knowledge.PlayerKnowledge.CircleOutcome;
import org.junit.jupiter.api.Test;

class PlayerKnowledgeTest {
    private static final Identifier ENTITIES = Identifier.withDefaultNamespace("entity_type");
    private static final Identifier OAK_LOG = Identifier.parse("minecraft:oak_log");
    private static final Identifier ZOMBIE = Identifier.parse("minecraft:zombie");
    private static final Identifier HERBA = Identifier.parse("thaumory:herba");
    private static final Identifier AER = Identifier.parse("thaumory:aer");
    private static final Identifier ARCANUM = Identifier.parse("thaumory:arcanum");
    private static final Identifier CHAPTER = Identifier.parse("thaumory:first_crucible");
    private static final Identifier HINT = Identifier.parse("thaumory:arcanum_bends_space");

    private static final CircleCombination TELEPORT = new CircleCombination(ARCANUM, AER, Optional.of(HERBA));

    @Test
    void keepsScansApartPerRegistry() {
        PlayerKnowledge knowledge = PlayerKnowledge.EMPTY
                .withScanned(PlayerKnowledge.ITEMS, OAK_LOG)
                .withScanned(ENTITIES, ZOMBIE);

        assertTrue(knowledge.hasScanned(PlayerKnowledge.ITEMS, OAK_LOG));
        assertFalse(knowledge.hasScanned(PlayerKnowledge.ITEMS, ZOMBIE));
        assertEquals(Set.of(ZOMBIE), knowledge.scanned(ENTITIES));
    }

    @Test
    void returnsSameInstanceWhenNothingChanges() {
        PlayerKnowledge knowledge = PlayerKnowledge.EMPTY
                .withScanned(PlayerKnowledge.ITEMS, OAK_LOG)
                .withAspect(HERBA)
                .withCircle(TELEPORT, CircleOutcome.SUCCESS)
                .withChapter(CHAPTER)
                .withHint(HINT);

        assertSame(knowledge, knowledge.withScanned(PlayerKnowledge.ITEMS, OAK_LOG));
        assertSame(knowledge, knowledge.withAspect(HERBA));
        assertSame(knowledge, knowledge.withCircle(TELEPORT, CircleOutcome.SUCCESS));
        assertSame(knowledge, knowledge.withCircle(TELEPORT, CircleOutcome.FAILURE));
        assertSame(knowledge, knowledge.withChapter(CHAPTER));
        assertSame(knowledge, knowledge.withHint(HINT));
    }

    @Test
    void successIsNeverOverwrittenByFailure() {
        PlayerKnowledge failed = PlayerKnowledge.EMPTY.withCircle(TELEPORT, CircleOutcome.FAILURE);
        assertEquals(Optional.of(CircleOutcome.FAILURE), failed.circle(TELEPORT));

        PlayerKnowledge succeeded = failed.withCircle(TELEPORT, CircleOutcome.SUCCESS);
        assertEquals(Optional.of(CircleOutcome.SUCCESS), succeeded.circle(TELEPORT));
        assertEquals(Optional.of(CircleOutcome.SUCCESS), succeeded.withCircle(TELEPORT, CircleOutcome.FAILURE).circle(TELEPORT));
    }

    @Test
    void effectSlotsAreUnordered() {
        assertEquals(new CircleCombination(AER, ARCANUM, Optional.empty()), new CircleCombination(ARCANUM, AER, Optional.empty()));
        assertFalse(new CircleCombination(AER, ARCANUM, Optional.empty()).equals(TELEPORT));
    }

    @Test
    void roundTripsThroughCodec() {
        PlayerKnowledge knowledge = sample();
        var json = PlayerKnowledge.CODEC.encodeStart(JsonOps.INSTANCE, knowledge).getOrThrow();
        assertEquals(knowledge, PlayerKnowledge.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void roundTripsThroughStreamCodec() {
        PlayerKnowledge knowledge = sample();
        ByteBuf buf = Unpooled.buffer();
        PlayerKnowledge.STREAM_CODEC.encode(buf, knowledge);
        assertEquals(knowledge, PlayerKnowledge.STREAM_CODEC.decode(buf));
    }

    @Test
    void loadsSavesWithMissingFields() {
        PlayerKnowledge knowledge = PlayerKnowledge.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{ \"aspects\": [\"thaumory:herba\"] }")).getOrThrow();

        assertEquals(PlayerKnowledge.EMPTY.withAspect(HERBA), knowledge);
    }

    @Test
    void keepsSuccessWhenASaveListsBothOutcomes() {
        PlayerKnowledge knowledge = PlayerKnowledge.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                { "circles": [
                  { "combination": { "first": "thaumory:aer", "second": "thaumory:arcanum" }, "outcome": "success" },
                  { "combination": { "first": "thaumory:arcanum", "second": "thaumory:aer" }, "outcome": "failure" }
                ] }""")).getOrThrow();

        assertEquals(Optional.of(CircleOutcome.SUCCESS), knowledge.circle(new CircleCombination(AER, ARCANUM, Optional.empty())));
    }

    private static PlayerKnowledge sample() {
        return PlayerKnowledge.EMPTY
                .withScanned(PlayerKnowledge.ITEMS, OAK_LOG)
                .withScanned(ENTITIES, ZOMBIE)
                .withAspect(HERBA)
                .withAspect(AER)
                .withCircle(TELEPORT, CircleOutcome.SUCCESS)
                .withCircle(new CircleCombination(HERBA, AER, Optional.empty()), CircleOutcome.FAILURE)
                .withChapter(CHAPTER)
                .withHint(HINT);
    }
}
