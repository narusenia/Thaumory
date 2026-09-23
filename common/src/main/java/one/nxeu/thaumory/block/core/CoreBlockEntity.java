package one.nxeu.thaumory.block.core;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.chalk.ChalkPatternBlock;
import one.nxeu.thaumory.circle.CircleScan;
import one.nxeu.thaumory.circle.CircleSettings;
import one.nxeu.thaumory.item.RuneItem;

/**
 * A magic circle's Core: up to three runes, kept by aspect id in slot order, and the chalk around
 * it as last scanned. The scan is not saved; it runs again every {@code scan_interval} ticks.
 */
public final class CoreBlockEntity extends BlockEntity {
    public static final int SLOTS = 3;
    private static final Codec<List<Identifier>> RUNES_CODEC = Identifier.CODEC.listOf(0, SLOTS);
    private static final CircleScan UNSCANNED = new CircleScan(0, List.of(), List.of());
    private static volatile CircleSettings settings = CircleSettings.DEFAULT;

    private List<Identifier> runes = List.of();
    private CircleScan scan = UNSCANNED;
    private int instability;

    public CoreBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.CORE_ENTITY.get(), pos, state);
    }

    public static void updateSettings(CircleSettings newSettings) {
        settings = newSettings;
    }

    public static CircleSettings settings() {
        return settings;
    }

    /** Rescans on the interval. The offset by position keeps many Cores from scanning on the same tick. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CoreBlockEntity core) {
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), settings.scanInterval()) == 0) {
            core.rescan();
        }
    }

    public void rescan() {
        if (level == null) {
            return;
        }
        Identifier line = BuiltInRegistries.BLOCK.getKey(ThaumoryBlocks.CHALK_LINE.get());
        scan = CircleScan.scan((dx, dz) -> {
            Block block = level.getBlockState(worldPosition.offset(dx, 0, dz)).getBlock();
            return block instanceof ChalkPatternBlock ? Optional.of(BuiltInRegistries.BLOCK.getKey(block)) : Optional.empty();
        }, line);
        instability = settings.instability(scan.nodes());
    }

    public CircleScan scan() {
        return scan;
    }

    public int instability() {
        return instability;
    }

    /** Aspect ids of the runes, slot 1 first. */
    public List<Identifier> runes() {
        return runes;
    }

    /** Puts a rune in the first empty slot. False when all are full. */
    public boolean insert(Identifier aspect) {
        if (runes.size() >= SLOTS) {
            return false;
        }
        List<Identifier> updated = new ArrayList<>(runes);
        updated.add(aspect);
        setRunes(updated);
        return true;
    }

    /** Takes the rune out of the last filled slot. */
    public Optional<Identifier> removeLast() {
        if (runes.isEmpty()) {
            return Optional.empty();
        }
        Identifier last = runes.getLast();
        setRunes(runes.subList(0, runes.size() - 1));
        return Optional.of(last);
    }

    private void setRunes(List<Identifier> updated) {
        runes = List.copyOf(updated);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel) {
            for (Identifier aspect : runes) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RuneItem.of(aspect));
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!runes.isEmpty()) {
            output.store("runes", RUNES_CODEC, runes);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        runes = input.read("runes", RUNES_CODEC).orElse(List.of());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
