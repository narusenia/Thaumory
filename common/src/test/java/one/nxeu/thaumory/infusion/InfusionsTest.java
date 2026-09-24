package one.nxeu.thaumory.infusion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class InfusionsTest {
    private static final Identifier HEALING = Identifier.parse("thaumory:healing");
    private static final Identifier TELEPORT = Identifier.parse("thaumory:teleport");
    private static final Identifier TERRA = Identifier.parse("thaumory:terra");

    @Test
    void theSameEffectIsReplacedAndOthersAdded() {
        Infusions infusions = Infusions.EMPTY
                .with(new Infusion(HEALING, 1, Optional.empty()))
                .with(new Infusion(TELEPORT, 1, Optional.of(TERRA)))
                .with(new Infusion(HEALING, 3, Optional.empty()));

        assertEquals(List.of(new Infusion(HEALING, 3, Optional.empty()), new Infusion(TELEPORT, 1, Optional.of(TERRA))), infusions.list());
    }

    @Test
    void roundTripsThroughCodecs() {
        Infusions infusions = Infusions.EMPTY.with(new Infusion(TELEPORT, 2, Optional.of(TERRA)));

        var json = Infusions.CODEC.encodeStart(JsonOps.INSTANCE, infusions).getOrThrow();
        assertEquals(infusions, Infusions.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        ByteBuf buf = Unpooled.buffer();
        Infusions.STREAM_CODEC.encode(buf, infusions);
        assertEquals(infusions, Infusions.STREAM_CODEC.decode(buf));
    }
}
