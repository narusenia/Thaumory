package one.nxeu.thaumory.particle;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;

/** Thaumory's particles, and the server side of sending them. */
public final class ThaumoryParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Thaumory.MOD_ID, Registries.PARTICLE_TYPE);

    /** A small glowing mote in an aspect's colour that drifts up and fades (requirements §4.5). */
    public static final RegistrySupplier<ParticleType<ColorParticleOption>> ASPECT_MOTE = TYPES.register("aspect_mote",
            () -> new ParticleType<ColorParticleOption>(false) {
                @Override
                public MapCodec<ColorParticleOption> codec() {
                    return ColorParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                    return ColorParticleOption.streamCodec(this);
                }
            });

    private static final int BLOCK_MOTES = 2;
    private static final int ENTITY_MOTES = 3;

    private ThaumoryParticles() {}

    public static void register() {
        TYPES.register();
    }

    /**
     * Motes rising from a block, each in the colour of one of {@code aspects} picked at random. They
     * start on top of a full block and inside anything smaller (a crop, say).
     */
    public static void motes(ServerLevel level, BlockPos pos, List<Aspect> aspects) {
        double y = level.getBlockState(pos).isCollisionShapeFullBlock(level, pos) ? pos.getY() + 1.05 : pos.getY() + 0.5;
        motes(level, pos.getX() + 0.5, y, pos.getZ() + 0.5, 0.3, 0.15, aspects, BLOCK_MOTES);
    }

    /** Motes rising from around an entity, each in the colour of one of {@code aspects} picked at random. */
    public static void motes(ServerLevel level, Entity entity, List<Aspect> aspects) {
        double width = entity.getBbWidth() / 2;
        double height = entity.getBbHeight() / 2;
        motes(level, entity.getX(), entity.getY() + height, entity.getZ(), width * 0.8, height * 0.8, aspects, ENTITY_MOTES);
    }

    private static void motes(ServerLevel level, double x, double y, double z, double across, double up, List<Aspect> aspects, int count) {
        if (aspects.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            Aspect aspect = aspects.get(random.nextInt(aspects.size()));
            ColorParticleOption mote = ColorParticleOption.create(ASPECT_MOTE.get(), 0xFF000000 | aspect.color());
            level.sendParticles(mote, x, y, z, 1, across, up, across, 0);
        }
    }
}
