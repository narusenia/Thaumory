package one.nxeu.thaumory.api.flux;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Flux stored per chunk. It never spreads to other chunks and decays naturally over game time,
 * so amounts read later are lower than what was added. Amounts below 1 count as none.
 *
 * <p>Every method loads the chunk if it is not loaded yet, so call them for chunks near where
 * something is happening.
 */
public interface ChunkFlux {
    /** The current amount, after natural decay. */
    double get(ServerLevel level, ChunkPos chunk);

    /** The stage the current amount has reached. */
    FluxStage stage(ServerLevel level, ChunkPos chunk);

    /**
     * Adds Flux to the chunk.
     *
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    void add(ServerLevel level, ChunkPos chunk, double amount);

    /**
     * Removes up to {@code amount} of Flux from the chunk.
     *
     * @return how much was actually removed
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    double remove(ServerLevel level, ChunkPos chunk, double amount);
}
