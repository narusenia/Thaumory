package one.nxeu.thaumory.block.pedestal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import one.nxeu.thaumory.block.ThaumoryBlocks;

/** Holds the one item on a pedestal, which the client sees too so it can draw it. */
public final class PedestalBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.PEDESTAL_ENTITY.get(), pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack stack) {
        item = stack;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Takes the item off, leaving the pedestal empty. */
    public ItemStack take() {
        ItemStack taken = item;
        setItem(ItemStack.EMPTY);
        return taken;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel && !item.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, item);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!item.isEmpty()) {
            output.store("item", ItemStack.CODEC, item);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        item = input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Written out even when empty, so the client clears an item taken off. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
