package one.nxeu.thaumory.flux;

import java.util.Optional;
import net.minecraft.world.level.chunk.LevelChunk;

/** Saves a {@link FluxValue} with a chunk. Each loader implements it with its own chunk data API. */
public interface FluxStorage {
    Optional<FluxValue> get(LevelChunk chunk);

    /** Stores the value, or removes the chunk's data when it is empty. Marks the chunk for saving. */
    void set(LevelChunk chunk, Optional<FluxValue> value);
}
