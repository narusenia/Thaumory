package one.nxeu.thaumory.block.jar;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.essentia.EssentiaContainer;
import one.nxeu.thaumory.aspect.AspectCancellation;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.jar.EssentiaTransfer;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.jar.JarSettings;

/** A placed jar. Its contents travel with the item when broken and come back when placed. */
public final class JarBlockEntity extends BlockEntity {
    private static final Codec<JarContents> CODEC = JarContents.codec(ThaumoryApi.aspects());
    private static volatile JarSettings settings = JarSettings.DEFAULT;

    private JarContents contents = JarContents.EMPTY;
    /** The server's capacity, as last received. Only meaningful on the client. */
    private int clientCapacity = JarSettings.DEFAULT.capacity();

    public JarBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.JAR_ENTITY.get(), pos, state);
    }

    public static void updateSettings(JarSettings newSettings) {
        settings = newSettings;
    }

    public static int capacity() {
        return settings.capacity();
    }

    /** The capacity to show: the server's, even on the client. */
    public int displayCapacity() {
        return level != null && level.isClientSide() ? clientCapacity : capacity();
    }

    /**
     * For pipes and other mods: a labeled jar lets only its label's aspect in and out. An unlabeled
     * one takes anything and cancels opposites into Flux once a transfer is final (requirements §8.2).
     */
    private final EssentiaContainer container = new EssentiaContainer() {
        @Override
        public AspectList contents() {
            return contents.aspects();
        }

        @Override
        public int space(AspectList held, Aspect aspect) {
            return EssentiaTransfer.jarSpace(held, contents.label(), capacity(), aspect);
        }

        @Override
        public boolean canExtract(Aspect aspect) {
            return contents.label().map(aspect::equals).orElse(true);
        }

        @Override
        public void update(AspectList held) {
            AspectCancellation.Result settled = EssentiaTransfer.settleJar(held, contents.label(), ThaumoryApi.aspects());
            setContents(contents.withAspects(settled.remaining()));
            if (settled.removed() > 0 && level instanceof ServerLevel server) {
                ThaumoryApi.flux().add(server, ChunkPos.containing(worldPosition), settled.removed());
            }
        }
    };

    public EssentiaContainer container() {
        return container;
    }

    public JarContents contents() {
        return contents;
    }

    public void setContents(JarContents newContents) {
        if (newContents.equals(contents)) {
            return;
        }
        contents = newContents;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            boolean labeled = contents.label().isPresent();
            if (state.getValue(JarBlock.LABELED) != labeled) {
                level.setBlock(worldPosition, state.setValue(JarBlock.LABELED, labeled), Block.UPDATE_CLIENTS);
            }
            level.sendBlockUpdated(worldPosition, state, getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!contents.isEmpty()) {
            output.store("contents", CODEC, contents);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        contents = input.read("contents", CODEC).orElse(JarContents.EMPTY);
        clientCapacity = input.getIntOr("capacity", clientCapacity);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        contents = components.getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!contents.isEmpty()) {
            components.set(ThaumoryComponents.JAR_CONTENTS.get(), contents);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("contents");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putInt("capacity", capacity());
        return tag;
    }
}
