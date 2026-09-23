package one.nxeu.thaumory.flux;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import one.nxeu.thaumory.api.flux.ChunkFlux;
import one.nxeu.thaumory.api.flux.FluxStage;

/** Built-in {@link ChunkFlux}: {@link FluxValue} math over a loader's {@link FluxStorage}. */
public final class FluxManager implements ChunkFlux {
    private final FluxStorage storage;
    private volatile FluxSettings settings = FluxSettings.DEFAULT;

    public FluxManager(FluxStorage storage) {
        this.storage = storage;
    }

    public FluxSettings settings() {
        return settings;
    }

    public void updateSettings(FluxSettings newSettings) {
        settings = newSettings;
    }

    @Override
    public double get(ServerLevel level, ChunkPos chunk) {
        return FluxValue.amountAt(storage.get(chunk(level, chunk)), level.getGameTime(), settings);
    }

    @Override
    public FluxStage stage(ServerLevel level, ChunkPos chunk) {
        return settings.stage(get(level, chunk));
    }

    @Override
    public void add(ServerLevel level, ChunkPos chunk, double amount) {
        requireNonNegative(amount);
        LevelChunk target = chunk(level, chunk);
        long now = level.getGameTime();
        Optional<FluxValue> stored = storage.get(target);
        store(target, stored, FluxValue.of(FluxValue.amountAt(stored, now, settings) + amount, now));
    }

    @Override
    public double remove(ServerLevel level, ChunkPos chunk, double amount) {
        requireNonNegative(amount);
        LevelChunk target = chunk(level, chunk);
        long now = level.getGameTime();
        Optional<FluxValue> stored = storage.get(target);
        double current = FluxValue.amountAt(stored, now, settings);
        double removed = Math.min(current, amount);
        store(target, stored, FluxValue.of(current - removed, now));
        return removed;
    }

    /** Replaces the chunk's Flux outright. For debugging; effects should use {@link #add} and {@link #remove}. */
    public void set(ServerLevel level, ChunkPos chunk, double amount) {
        requireNonNegative(amount);
        LevelChunk target = chunk(level, chunk);
        store(target, storage.get(target), FluxValue.of(amount, level.getGameTime()));
    }

    /** Leaves chunks without Flux untouched, so they are not marked for saving. */
    private void store(LevelChunk chunk, Optional<FluxValue> before, Optional<FluxValue> after) {
        if (before.isPresent() || after.isPresent()) {
            storage.set(chunk, after);
        }
    }

    private static LevelChunk chunk(ServerLevel level, ChunkPos chunk) {
        return level.getChunk(chunk.x(), chunk.z());
    }

    private static void requireNonNegative(double amount) {
        if (!(amount >= 0)) {
            throw new IllegalArgumentException("Flux amount must not be negative: " + amount);
        }
    }
}
