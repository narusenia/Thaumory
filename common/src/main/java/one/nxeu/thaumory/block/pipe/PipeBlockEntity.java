package one.nxeu.thaumory.block.pipe;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.pipe.PipeDisplay;
import one.nxeu.thaumory.pipe.PipeNetworks;

/**
 * Keeps this pipe's share of its network's Essentia in transit, so it survives saving. While the
 * network runs, the network holds the Essentia and this share is only what was last saved. A
 * filter pipe also keeps its filter here, which the client gets too, for the band's color.
 */
public final class PipeBlockEntity extends BlockEntity {
    private static final Codec<AspectList> CODEC = AspectCodecs.aspectList(ThaumoryApi.aspects());

    private AspectList share = AspectList.empty();
    /** Kept as an id, so a filter whose addon was removed comes back with it. */
    private Optional<Identifier> filter = Optional.empty();
    /** What the client draws inside the glass; worked out by the network, never saved. */
    private PipeDisplay display = PipeDisplay.EMPTY;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.PIPE_ENTITY.get(), pos, state);
    }

    /** What this pipe holds while no network has taken it up. */
    public AspectList share() {
        return share;
    }

    /** A filter pipe's aspect; empty for any other pipe, or a filter pipe with no filter. */
    public Optional<Aspect> filter() {
        return filter.flatMap(ThaumoryApi.aspects()::get);
    }

    public void setFilter(Optional<Aspect> aspect) {
        Optional<Identifier> id = aspect.map(Aspect::id);
        if (id.equals(filter)) {
            return;
        }
        filter = id;
        setChanged();
        if (level instanceof ServerLevel server) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            PipeNetworks.of(server).invalidate(worldPosition);
        }
    }

    public PipeDisplay display() {
        return display;
    }

    /** Sent to the client only when it changes. */
    public void setDisplay(PipeDisplay next) {
        if (!next.equals(display)) {
            display = next;
            if (level instanceof ServerLevel) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Called by the network when it lets go of this pipe. */
    public void setShare(AspectList newShare) {
        if (!newShare.equals(share)) {
            share = newShare;
            setChanged();
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel server) {
            PipeNetworks.of(server).invalidate(worldPosition);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) {
            PipeNetworks.of(server).invalidate(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel server) {
            PipeNetworks.of(server).removed(worldPosition, this);
        }
        super.setRemoved();
    }

    /** Broken, not just unloaded: what this pipe carried leaks out as Flux (requirements §8.2). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            int leaked = PipeNetworks.of(server).breakPipe(pos, this).total();
            if (leaked > 0) {
                ThaumoryApi.flux().add(server, ChunkPos.containing(pos), leaked);
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        AspectList current = level instanceof ServerLevel server ? PipeNetworks.of(server).shareOf(worldPosition).orElse(share) : share;
        if (!current.isEmpty()) {
            output.store("share", CODEC, current);
        }
        filter.ifPresent(id -> output.store("filter", Identifier.CODEC, id));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        share = input.read("share", CODEC).orElse(AspectList.empty());
        display = new PipeDisplay(input.getIntOr("fill_color", PipeDisplay.EMPTY.color()), input.getIntOr("fill_level", 0));
        Optional<Identifier> loaded = input.read("filter", Identifier.CODEC);
        boolean changed = !loaded.equals(filter);
        filter = loaded;
        // A new filter from the server: draw the band again in its color.
        if (changed && level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    /** Only the filter and what to draw go to the client; the share is the server's business. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        filter.ifPresent(id -> tag.putString("filter", id.toString()));
        if (display.level() > 0) {
            tag.putInt("fill_color", display.color());
            tag.putInt("fill_level", display.level());
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
