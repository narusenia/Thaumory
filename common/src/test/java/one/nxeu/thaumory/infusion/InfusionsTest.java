package one.nxeu.thaumory.infusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private static final Identifier LIGHT = Identifier.parse("thaumory:light");
    private static final Identifier TERRA = Identifier.parse("thaumory:terra");

    @Test
    void theSameEffectIsReplacedAndOthersAdded() {
        Infusions infusions = Infusions.EMPTY
                .with(new Infusion(HEALING, 1, Optional.empty(), 2))
                .with(new Infusion(TELEPORT, 1, Optional.of(TERRA), 3))
                .with(new Infusion(HEALING, 3, Optional.empty(), 2));

        assertEquals(List.of(new Infusion(HEALING, 3, Optional.empty(), 2), new Infusion(TELEPORT, 1, Optional.of(TERRA), 3)),
                infusions.list());
        assertEquals(5, infusions.used());
    }

    @Test
    void anEffectFitsOnlyWithinTheCapacity() {
        Infusions infusions = Infusions.EMPTY.with(new Infusion(HEALING, 1, Optional.empty(), 2));

        assertTrue(infusions.fits(new Infusion(LIGHT, 1, Optional.empty(), 1), 3));
        assertFalse(infusions.fits(new Infusion(TELEPORT, 1, Optional.of(TERRA), 3), 3));
        assertTrue(infusions.fits(new Infusion(TELEPORT, 1, Optional.of(TERRA), 3), 5));
    }

    @Test
    void replacingAnEffectDoesNotCountItsOldShare() {
        Infusions infusions = Infusions.EMPTY
                .with(new Infusion(HEALING, 1, Optional.empty(), 2))
                .with(new Infusion(LIGHT, 1, Optional.empty(), 1));

        assertTrue(infusions.fits(new Infusion(HEALING, 2, Optional.empty(), 2), 3));
        assertFalse(infusions.fits(new Infusion(HEALING, 2, Optional.empty(), 3), 3));
    }

    @Test
    void anItemWithoutCapacityTakesNothing() {
        assertFalse(Infusions.EMPTY.fits(new Infusion(LIGHT, 1, Optional.empty(), 1), 0));
    }

    @Test
    void roundTripsThroughCodecs() {
        Infusions infusions = Infusions.EMPTY.with(new Infusion(TELEPORT, 2, Optional.of(TERRA), 3));

        var json = Infusions.CODEC.encodeStart(JsonOps.INSTANCE, infusions).getOrThrow();
        assertEquals(infusions, Infusions.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        ByteBuf buf = Unpooled.buffer();
        Infusions.STREAM_CODEC.encode(buf, infusions);
        assertEquals(infusions, Infusions.STREAM_CODEC.decode(buf));
    }
}
