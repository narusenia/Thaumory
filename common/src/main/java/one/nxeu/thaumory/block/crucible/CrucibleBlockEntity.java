package one.nxeu.thaumory.block.crucible;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.alchemy.AlchemyInput;
import one.nxeu.thaumory.alchemy.AlchemyRecipe;
import one.nxeu.thaumory.alchemy.AlchemySelection;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.crucible.CrucibleSettings;
import one.nxeu.thaumory.crucible.CrucibleTank;
import one.nxeu.thaumory.research.ResearchProgress;
import one.nxeu.thaumory.item.ThaumoryComponents;

/**
 * Heats up while there is water and a heat source below, then melts the items inside one at a
 * time. Contents are sent to clients so the Arcane Loupe can show them.
 */
public final class CrucibleBlockEntity extends BlockEntity {
    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, Thaumory.id("crucible_heat_sources"));

    private static final Codec<CrucibleTank> TANK_CODEC = CrucibleTank.codec(AspectCodecs.aspectList(ThaumoryApi.aspects()));
    private static volatile CrucibleSettings settings = CrucibleSettings.DEFAULT;

    private CrucibleTank tank = CrucibleTank.EMPTY;
    private int heat;
    private int meltCooldown;
    private int cancelCooldown;
    /** The server's capacity, as last received. Only meaningful on the client. */
    private int clientCapacity = CrucibleSettings.DEFAULT.capacity();

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ThaumoryBlocks.CRUCIBLE_ENTITY.get(), pos, state);
    }

    public static void updateSettings(CrucibleSettings newSettings) {
        settings = newSettings;
    }

    public CrucibleTank tank() {
        return tank;
    }

    public int capacity() {
        return level != null && level.isClientSide() ? clientCapacity : settings.capacity();
    }

    public static boolean isHeatSource(BlockState state) {
        return state.is(HEAT_SOURCES) && (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT));
    }

    void setWater(int water) {
        setTank(tank.withWater(water));
    }

    /** Replaces the Essentia, keeping the water. For jars drawing from or pouring into the Crucible. */
    public void setContents(AspectList contents) {
        setTank(new CrucibleTank(contents, tank.water(), tank.essentiaSinceWaterDrop()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        CrucibleSettings current = settings;
        boolean heating = crucible.tank.hasWater() && isHeatSource(level.getBlockState(pos.below()));
        crucible.heat = heating ? Math.min(crucible.heat + 1, current.boilTime()) : 0;
        boolean boiling = heating && crucible.heat >= current.boilTime();
        if (state.getValue(CrucibleBlock.BOILING) != boiling) {
            level.setBlock(pos, state.setValue(CrucibleBlock.BOILING, boiling), Block.UPDATE_CLIENTS);
        }
        if (!boiling) {
            return;
        }
        if (++crucible.cancelCooldown >= current.cancelInterval()) {
            crucible.cancelCooldown = 0;
            crucible.cancelOpposites((ServerLevel) level, current);
        }
        if (++crucible.meltCooldown >= current.meltInterval()) {
            crucible.meltCooldown = 0;
            crucible.meltOne((ServerLevel) level, current);
        }
    }

    /** Opposite aspects wear each other down; what they lose becomes Flux. */
    private void cancelOpposites(ServerLevel level, CrucibleSettings current) {
        CrucibleTank.CancelResult result = tank.cancel(ThaumoryApi.aspects(), current);
        if (result.flux() == 0) {
            return;
        }
        setTank(result.tank());
        ThaumoryApi.flux().add(level, ChunkPos.containing(worldPosition), result.flux());
        level.sendParticles(ParticleTypes.WITCH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5,
                4, 0.25, 0.05, 0.25, 0);
        level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.15f, 1.6f);
    }

    /**
     * Takes one item from the first stack inside that can be used: as the catalyst of an alchemy
     * recipe the contents can pay for, or else melted if it has aspects.
     */
    private void meltOne(ServerLevel level, CrucibleSettings current) {
        AABB inside = CrucibleBlock.INSIDE.bounds().move(worldPosition);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, inside, ItemEntity::isAlive);
        for (ItemEntity entity : items) {
            ItemStack stack = entity.getItem();
            Optional<AlchemyRecipe> alchemy = alchemyFor(level, entity);
            if (alchemy.isPresent()) {
                transmute(level, entity, alchemy.get());
                return;
            }
            AspectList aspects = ItemAspects.get(stack);
            // A jar holding Essentia would take it into the melt unseen; leave it for the player to empty.
            if (aspects.isEmpty() || stack.has(ThaumoryComponents.JAR_CONTENTS.get())) {
                continue;
            }
            double ratio = ItemAspects.source(stack.getItem()) == ItemAspects.Source.DATAPACK
                    ? current.meltRatio().manual()
                    : current.meltRatio().estimated();
            CrucibleTank.MeltResult result = tank.melt(aspects, ratio, current);

            ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
            stack.shrink(1);
            if (stack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
            if (remainder != null) {
                popOut(level, remainder.create());
            }
            if (result.overflow() > 0) {
                ThaumoryApi.flux().add(level, ChunkPos.containing(worldPosition), result.overflow());
            }
            setTank(result.tank());

            level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3f, 1.4f);
            level.sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY(), entity.getZ(), 6, 0.15, 0.05, 0.15, 0);
            return;
        }
    }

    /** The recipe this catalyst makes, among those the one who threw it in has unlocked. */
    private Optional<AlchemyRecipe> alchemyFor(ServerLevel level, ItemEntity entity) {
        ItemStack catalyst = entity.getItem();
        Optional<ServerPlayer> thrower = entity.getOwner() instanceof ServerPlayer player ? Optional.of(player) : Optional.empty();
        AlchemyInput input = new AlchemyInput(catalyst, tank.contents());
        List<AlchemySelection.Candidate<AlchemyRecipe>> candidates = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof AlchemyRecipe recipe && recipe.catalyst().test(catalyst)
                    && ResearchProgress.canUse(thrower, holder.id().identifier())) {
                candidates.add(new AlchemySelection.Candidate<>(holder.id().identifier(), recipe.aspects(), recipe));
            }
        }
        return AlchemySelection.choose(candidates, input.contents()).map(AlchemySelection.Candidate::recipe);
    }

    /** Uses up the recipe's aspects and one catalyst, and throws the result out. */
    private void transmute(ServerLevel level, ItemEntity catalyst, AlchemyRecipe recipe) {
        ItemStack stack = catalyst.getItem();
        ItemStack result = recipe.assemble(new AlchemyInput(stack, tank.contents()));
        stack.shrink(1);
        if (stack.isEmpty()) {
            catalyst.discard();
        } else {
            catalyst.setItem(stack);
        }
        setTank(new CrucibleTank(tank.contents().minus(recipe.aspects()), tank.water(), tank.essentiaSinceWaterDrop()));
        popOut(level, result);

        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.ENCHANT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.5);
    }

    /** Throws an item (an alchemy result, the bucket from a lava bucket) over the rim so it does not melt. */
    private void popOut(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, stack);
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        entity.setDeltaMovement(Math.cos(angle) * 0.2, 0.3, Math.sin(angle) * 0.2);
        level.addFreshEntity(entity);
    }

    private void setTank(CrucibleTank newTank) {
        if (newTank.equals(tank)) {
            return;
        }
        tank = newTank;
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            if (state.getValue(CrucibleBlock.LEVEL) != tank.water()) {
                level.setBlock(worldPosition, state.setValue(CrucibleBlock.LEVEL, tank.water()), Block.UPDATE_CLIENTS);
            }
            level.sendBlockUpdated(worldPosition, state, getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Breaking the Crucible spills everything in it as Flux. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server && tank.contents().total() > 0) {
            ThaumoryApi.flux().add(server, ChunkPos.containing(pos), tank.contents().total());
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("tank", TANK_CODEC, tank);
        output.putInt("heat", heat);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank = input.read("tank", TANK_CODEC).orElse(CrucibleTank.EMPTY);
        heat = input.getIntOr("heat", 0);
        clientCapacity = input.getIntOr("capacity", clientCapacity);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putInt("capacity", settings.capacity());
        return tag;
    }
}
