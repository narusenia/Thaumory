package one.nxeu.thaumory.fabric;

import java.util.Optional;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.level.chunk.LevelChunk;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.flux.FluxStorage;
import one.nxeu.thaumory.flux.FluxValue;

/** Keeps Flux in a persistent chunk attachment. Not synced; clients do not need it yet. */
final class FabricFluxStorage implements FluxStorage {
    private final AttachmentType<FluxValue> type =
            AttachmentRegistry.create(Thaumory.id("flux"), builder -> builder.persistent(FluxValue.CODEC));

    @Override
    public Optional<FluxValue> get(LevelChunk chunk) {
        return Optional.ofNullable(chunk.getAttached(type));
    }

    @Override
    public void set(LevelChunk chunk, Optional<FluxValue> value) {
        // Fabric marks the chunk unsaved on every change.
        chunk.setAttached(type, value.orElse(null));
    }
}
