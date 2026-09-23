package one.nxeu.thaumory.network;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.METALLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import org.junit.jupiter.api.Test;

class AspectSyncPayloadTest {
    private static final Identifier LOG = Identifier.parse("minecraft:oak_log");
    private static final Identifier INGOT = Identifier.parse("minecraft:iron_ingot");

    @Test
    void roundTrips() {
        AspectRegistry registry = registry();
        AspectSyncPayload payload = new AspectSyncPayload(Map.of(
                LOG, AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 8)),
                INGOT, AspectList.of(METALLUM, 300)));

        assertEquals(payload, roundTrip(payload, registry, registry));
    }

    @Test
    void dropsAspectsTheClientDoesNotKnow() {
        AspectRegistry server = registry();
        // The client lacks every compound, so Herba and Metallum are unknown there.
        AspectRegistry client = new AspectRegistry();
        List.of(ThaumoryAspects.IGNIS, ThaumoryAspects.AER, ThaumoryAspects.VITA,
                ThaumoryAspects.AQUA, TERRA, ThaumoryAspects.MORS).forEach(client::register);

        AspectSyncPayload payload = new AspectSyncPayload(Map.of(
                LOG, AspectList.of(new AspectStack(HERBA, 16), new AspectStack(TERRA, 8)),
                INGOT, AspectList.of(METALLUM, 4)));

        assertEquals(new AspectSyncPayload(Map.of(LOG, AspectList.of(TERRA, 8))), roundTrip(payload, server, client));
    }

    private static AspectSyncPayload roundTrip(AspectSyncPayload payload, AspectRegistry encoder, AspectRegistry decoder) {
        ByteBuf buf = Unpooled.buffer();
        AspectSyncPayload.codec(encoder).encode(buf, payload);
        return AspectSyncPayload.codec(decoder).decode(buf);
    }

    private static AspectRegistry registry() {
        AspectRegistry registry = new AspectRegistry();
        ThaumoryAspects.register(registry);
        return registry;
    }
}
