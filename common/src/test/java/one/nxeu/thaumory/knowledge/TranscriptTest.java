package one.nxeu.thaumory.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.knowledge.PlayerKnowledge.CircleOutcome;
import one.nxeu.thaumory.knowledge.Transcript.AspectTranscript;
import one.nxeu.thaumory.knowledge.Transcript.CircleTranscript;
import org.junit.jupiter.api.Test;

class TranscriptTest {
    private static final Identifier AER = Identifier.parse("thaumory:aer");
    private static final Identifier ARCANUM = Identifier.parse("thaumory:arcanum");
    private static final Identifier HERBA = Identifier.parse("thaumory:herba");
    private static final CircleCombination TELEPORT = new CircleCombination(ARCANUM, AER, Optional.of(HERBA));

    @Test
    void teachesAnAspect() {
        Transcript transcript = new AspectTranscript(HERBA);

        assertFalse(transcript.knownBy(PlayerKnowledge.EMPTY));
        PlayerKnowledge taught = transcript.teach(PlayerKnowledge.EMPTY);
        assertTrue(taught.knowsAspect(HERBA));
        assertTrue(transcript.knownBy(taught));
    }

    @Test
    void teachesACircleAsASuccess() {
        Transcript transcript = new CircleTranscript(TELEPORT);
        PlayerKnowledge failed = PlayerKnowledge.EMPTY.withCircle(TELEPORT, CircleOutcome.FAILURE);

        assertFalse(transcript.knownBy(failed));
        PlayerKnowledge taught = transcript.teach(failed);
        assertEquals(Optional.of(CircleOutcome.SUCCESS), taught.circle(TELEPORT));
        assertTrue(transcript.knownBy(taught));
    }

    @Test
    void aCircleNeedsTheSameParameter() {
        PlayerKnowledge knowledge = PlayerKnowledge.EMPTY.withCircle(TELEPORT, CircleOutcome.SUCCESS);

        assertFalse(new CircleTranscript(new CircleCombination(ARCANUM, AER, Optional.empty())).knownBy(knowledge));
        assertTrue(new CircleTranscript(new CircleCombination(AER, ARCANUM, Optional.of(HERBA))).knownBy(knowledge));
    }

    @Test
    void roundTripsThroughCodecs() {
        for (Transcript transcript : new Transcript[] {new AspectTranscript(HERBA), new CircleTranscript(TELEPORT)}) {
            var json = Transcript.CODEC.encodeStart(JsonOps.INSTANCE, transcript).getOrThrow();
            assertEquals(transcript, Transcript.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());

            ByteBuf buf = Unpooled.buffer();
            Transcript.STREAM_CODEC.encode(buf, transcript);
            assertEquals(transcript, Transcript.STREAM_CODEC.decode(buf));
        }
    }

    @Test
    void readsTheSavedForm() {
        Transcript transcript = Transcript.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"type": "circle", "combination": {"first": "thaumory:aer", "second": "thaumory:arcanum", "parameter": "thaumory:herba"}}
                """)).getOrThrow();

        assertEquals(new CircleTranscript(TELEPORT), transcript);
    }
}
