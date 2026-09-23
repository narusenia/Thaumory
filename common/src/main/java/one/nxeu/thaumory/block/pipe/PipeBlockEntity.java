package one.nxeu.thaumory.block.pipe;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.pipe.PipeNetworks;

/**
 * Keeps this pipe's share of its network's Essentia in transit, so it survives saving. While the
 * network runs, the network holds the Essentia and this share is only what was last saved.
 */
public final class PipeBlockEntity extends BlockEntity {
    private static final Codec<AspectList> CODEC = AspectCodecs.aspectList(ThaumoryApi.aspects());

    private AspectList share = AspectList.empty();

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.PIPE_ENTITY.get(), pos, state);
    }

    /** What this pipe holds while no network has taken it up. */
    public AspectList share() {
        return share;
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        AspectList current = level instanceof ServerLevel server ? PipeNetworks.of(server).shareOf(worldPosition).orElse(share) : share;
        if (!current.isEmpty()) {
            output.store("share", CODEC, current);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        share = input.read("share", CODEC).orElse(AspectList.empty());
    }
}
