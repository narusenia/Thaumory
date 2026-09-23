package one.nxeu.thaumory.flux;

import dev.architectury.event.events.common.TickEvent;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import one.nxeu.thaumory.api.flux.FluxStage;
import one.nxeu.thaumory.flux.pollution.PollutionRules;

/**
 * What each Flux stage does to the world near players (requirements §5.1): purple particles from
 * stagnation on and polluted blocks from erosion on. Chunks within {@link #RADIUS} chunks of a
 * player are looked at on each effect's interval.
 */
public final class FluxWorldEffects {
    public static final int RADIUS = 3;
    private static final int PARTICLE_INTERVAL = 40;

    private final FluxManager flux;

    public FluxWorldEffects(FluxManager flux) {
        this.flux = flux;
    }

    public void register() {
        TickEvent.SERVER_LEVEL_POST.register(this::tick);
    }

    private void tick(ServerLevel level) {
        FluxSettings.Effects effects = flux.settings().effects();
        long time = level.getGameTime();
        boolean particles = time % PARTICLE_INTERVAL == 0;
        boolean pollution = time % effects.pollutionInterval() == 0;
        if (!particles && !pollution) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ChunkPos chunk : chunksNearPlayers(level)) {
            FluxStage stage = flux.stage(level, chunk);
            if (stage == FluxStage.NONE) {
                continue;
            }
            if (particles) {
                for (int i = 0; i < effects.particles(stage); i++) {
                    BlockPos top = surface(level, chunk, random);
                    level.sendParticles(ParticleTypes.WITCH, top.getX() + 0.5, top.getY() + 1.3, top.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0);
                }
            }
            if (pollution) {
                for (int i = 0; i < effects.pollutionAttempts(stage); i++) {
                    pollute(level, surface(level, chunk, random));
                }
            }
        }
    }

    /** Loaded chunks within {@link #RADIUS} chunks of any player in {@code level}. */
    public static Set<ChunkPos> chunksNearPlayers(ServerLevel level) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    ChunkPos chunk = new ChunkPos(center.x() + dx, center.z() + dz);
                    if (level.hasChunk(chunk.x(), chunk.z())) {
                        chunks.add(chunk);
                    }
                }
            }
        }
        return chunks;
    }

    /** The topmost solid block of a random column of the chunk. */
    public static BlockPos surface(ServerLevel level, ChunkPos chunk, RandomSource random) {
        int x = chunk.getMinBlockX() + random.nextInt(16);
        int z = chunk.getMinBlockZ() + random.nextInt(16);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
    }

    /** Turns the block at {@code pos} into its polluted form, if it has one. */
    public static boolean pollute(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Optional<Block> polluted = PollutionRules.pollutedFor(state);
        if (polluted.isEmpty()) {
            return false;
        }
        level.setBlockAndUpdate(pos, polluted.get().defaultBlockState());
        return true;
    }

    /** Pollutes every block that can be within {@code radius} of {@code center}. */
    public static void polluteAround(ServerLevel level, BlockPos center, double radius) {
        int r = (int) Math.ceil(radius);
        double limit = radius * radius;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (pos.distSqr(center) <= limit) {
                pollute(level, pos);
            }
        }
    }
}
