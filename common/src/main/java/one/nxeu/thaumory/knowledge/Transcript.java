package one.nxeu.thaumory.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import one.nxeu.thaumory.knowledge.PlayerKnowledge.CircleOutcome;

/**
 * One piece of knowledge copied out of the book, to be handed to another player (requirements
 * §6.3): an aspect worked out, or a circle combination seen to work.
 */
public sealed interface Transcript {
    Codec<Transcript> CODEC = Kind.CODEC.dispatch("type", Transcript::kind, Kind::codec);
    StreamCodec<ByteBuf, Transcript> STREAM_CODEC = ByteBufCodecs.BOOL.dispatch(
            transcript -> transcript.kind() == Kind.CIRCLE,
            circle -> circle ? CircleTranscript.STREAM_CODEC : AspectTranscript.STREAM_CODEC);

    Kind kind();

    /** Whether {@code knowledge} already holds this, so it can be copied, and needs no teaching. */
    boolean knownBy(PlayerKnowledge knowledge);

    /** {@code knowledge} with this learned, as if the player had found it themselves. */
    PlayerKnowledge teach(PlayerKnowledge knowledge);

    record AspectTranscript(Identifier aspect) implements Transcript {
        static final MapCodec<AspectTranscript> CODEC =
                Identifier.CODEC.fieldOf("aspect").xmap(AspectTranscript::new, AspectTranscript::aspect);
        static final StreamCodec<ByteBuf, Transcript> STREAM_CODEC =
                Identifier.STREAM_CODEC.map(AspectTranscript::new, transcript -> ((AspectTranscript) transcript).aspect());

        @Override
        public Kind kind() {
            return Kind.ASPECT;
        }

        @Override
        public boolean knownBy(PlayerKnowledge knowledge) {
            return knowledge.knowsAspect(aspect);
        }

        @Override
        public PlayerKnowledge teach(PlayerKnowledge knowledge) {
            return knowledge.withAspect(aspect);
        }
    }

    /** Only a success is ever copied, so the one taught learns that it works. */
    record CircleTranscript(CircleCombination combination) implements Transcript {
        static final MapCodec<CircleTranscript> CODEC =
                CircleCombination.CODEC.fieldOf("combination").xmap(CircleTranscript::new, CircleTranscript::combination);
        static final StreamCodec<ByteBuf, Transcript> STREAM_CODEC =
                CircleCombination.STREAM_CODEC.map(CircleTranscript::new, transcript -> ((CircleTranscript) transcript).combination());

        @Override
        public Kind kind() {
            return Kind.CIRCLE;
        }

        @Override
        public boolean knownBy(PlayerKnowledge knowledge) {
            return knowledge.circle(combination).filter(CircleOutcome.SUCCESS::equals).isPresent();
        }

        @Override
        public PlayerKnowledge teach(PlayerKnowledge knowledge) {
            return knowledge.withCircle(combination, CircleOutcome.SUCCESS);
        }
    }

    enum Kind implements StringRepresentable {
        ASPECT("aspect"),
        CIRCLE("circle");

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        MapCodec<? extends Transcript> codec() {
            return this == ASPECT ? AspectTranscript.CODEC : CircleTranscript.CODEC;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
