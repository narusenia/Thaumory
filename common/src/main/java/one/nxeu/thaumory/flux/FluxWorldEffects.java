package one.nxeu.thaumory.flux;

import dev.architectury.event.events.common.TickEvent;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.api.flux.FluxStage;
import one.nxeu.thaumory.entity.ThaumoryEntities;
import one.nxeu.thaumory.entity.VoidRemnant;
import one.nxeu.thaumory.flux.pollution.PollutionIndex;
import one.nxeu.thaumory.flux.pollution.PollutionRules;

/**
 * What each Flux stage does to the world near players (requirements §5.1): purple particles from
 * stagnation on, polluted blocks from erosion on and Void Remnants from manifestation on. Polluted
 * blocks keep adding Flux to their chunk, short of erosion. Chunks within {@link #RADIUS} chunks of a
 * player are looked at on each effect's interval.
 */
public final class FluxWorldEffects {
    public static final int RADIUS = 3;
    private static final int PARTICLE_INTERVAL = 40;

    private final FluxManager flux;
    /**
     * Flux from polluted blocks not yet added, by chunk. A chunk forgets Flux below 1, so small
     * amounts wait here until they reach 1. Lost on restart, which costs at most 1 per chunk.
     */
    private final Map<PendingKey, Double> pending = new HashMap<>();

    private record PendingKey(ResourceKey<Level> dimension, long chunk) {}

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
        boolean spawning = time % effects.spawnInterval() == 0;
        if (!particles && !pollution && !spawning) {
            return;
        }
        RandomSource random = level.getRandom();
        PollutionIndex pollutionIndex = PollutionIndex.of(level);
        for (ChunkPos chunk : chunksNearPlayers(level)) {
            if (pollution) {
                double current = flux.get(level, chunk);
                double gain = flux.settings().pollutedBlockGain(pollutionIndex.count(chunk), current);
                if (gain > 0) {
                    PendingKey key = new PendingKey(level.dimension(), chunk.pack());
                    double waiting = pending.getOrDefault(key, 0.0) + gain;
                    if (current >= 1 || waiting >= 1) {
                        flux.add(level, chunk, waiting);
                        pending.remove(key);
                    } else {
                        pending.put(key, waiting);
                    }
                }
            }
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
            if (spawning && effects.spawnsRemnants(stage) && random.nextDouble() < effects.spawnChance()) {
                spawnRemnant(level, chunk, random, effects.spawnCap());
            }
        }
    }

    /** One Void Remnant above the chunk's surface, unless it already holds {@code cap} of them. */
    private static void spawnRemnant(ServerLevel level, ChunkPos chunk, RandomSource random, int cap) {
        AABB column = new AABB(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1, level.getMaxY() + 1, chunk.getMaxBlockZ() + 1);
        if (level.getEntitiesOfClass(VoidRemnant.class, column).size() >= cap) {
            return;
        }
        BlockPos above = surface(level, chunk, random).above(2);
        if (level.getBlockState(above).isAir() && level.getBlockState(above.above()).isAir()) {
            ThaumoryEntities.VOID_REMNANT.get().spawn(level, above, EntitySpawnReason.EVENT);
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
