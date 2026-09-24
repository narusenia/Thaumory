package one.nxeu.thaumory.client;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import one.nxeu.thaumory.network.PipeReadingPayload;

/** The last reading of a pipe's network the server sent while the player looked at it through the loupe. */
public final class ClientPipeReading {
    /** A reading older than this belongs to a pipe the player has since looked away from. */
    private static final long STALE_MILLIS = 1000;

    private static volatile Optional<PipeReadingPayload> reading = Optional.empty();
    private static volatile long received;

    private ClientPipeReading() {}

    /** The reading for the pipe at {@code pos}, if a fresh one came for that pipe. */
    public static Optional<PipeReadingPayload> at(BlockPos pos) {
        return reading.filter(r -> r.pos().equals(pos) && System.currentTimeMillis() - received < STALE_MILLIS);
    }

    static void replace(PipeReadingPayload payload) {
        reading = Optional.of(payload);
        received = System.currentTimeMillis();
    }

    static void clear() {
        reading = Optional.empty();
    }
}
