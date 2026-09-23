package one.nxeu.thaumory.client;

import java.util.Optional;
import one.nxeu.thaumory.network.FluxReadingPayload;

/** The last Flux reading the server sent while the player held the loupe. */
public final class ClientFlux {
    private static volatile Optional<FluxReadingPayload> reading = Optional.empty();

    private ClientFlux() {}

    public static Optional<FluxReadingPayload> get() {
        return reading;
    }

    static void replace(FluxReadingPayload payload) {
        reading = Optional.of(payload);
    }

    static void clear() {
        reading = Optional.empty();
    }
}
