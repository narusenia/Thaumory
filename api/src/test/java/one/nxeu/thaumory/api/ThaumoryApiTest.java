package one.nxeu.thaumory.api;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import one.nxeu.thaumory.api.flux.ChunkFlux;
import one.nxeu.thaumory.api.flux.FluxStage;
import org.junit.jupiter.api.Test;

class ThaumoryApiTest {
    /** The provided implementation is global, so the whole lifecycle is checked in one test. */
    @Test
    void fluxIsProvidedExactlyOnce() {
        assertThrows(IllegalStateException.class, ThaumoryApi::flux);
        assertThrows(NullPointerException.class, () -> ThaumoryApi.provideFlux(null));

        ChunkFlux flux = new NoFlux();
        ThaumoryApi.provideFlux(flux);

        assertSame(flux, ThaumoryApi.flux());
        assertThrows(IllegalStateException.class, () -> ThaumoryApi.provideFlux(new NoFlux()));
    }

    private static final class NoFlux implements ChunkFlux {
        @Override
        public double get(ServerLevel level, ChunkPos chunk) {
            return 0;
        }

        @Override
        public FluxStage stage(ServerLevel level, ChunkPos chunk) {
            return FluxStage.NONE;
        }

        @Override
        public void add(ServerLevel level, ChunkPos chunk, double amount) {}

        @Override
        public double remove(ServerLevel level, ChunkPos chunk, double amount) {
            return 0;
        }
    }
}
