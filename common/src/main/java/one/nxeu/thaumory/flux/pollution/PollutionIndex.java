package one.nxeu.thaumory.flux.pollution;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import one.nxeu.thaumory.Thaumory;

/**
 * Where the polluted blocks of a dimension are, by chunk, saved with the world. Polluted blocks keep
 * it up to date as they are placed and removed; readers still check the block that is really there.
 */
public final class PollutionIndex extends SavedData {
    private static final Codec<PollutionIndex> CODEC = BlockPos.CODEC.listOf().xmap(positions -> {
        PollutionIndex index = new PollutionIndex();
        positions.forEach(index::put);
        return index;
    }, index -> index.chunks.values().stream().flatMap(Set::stream).toList());

    private static final SavedDataType<PollutionIndex> TYPE =
            new SavedDataType<>(Thaumory.id("pollution"), PollutionIndex::new, CODEC, DataFixTypes.LEVEL);

    private final Map<Long, Set<BlockPos>> chunks = new HashMap<>();

    public static PollutionIndex of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private boolean put(BlockPos pos) {
        return chunks.computeIfAbsent(ChunkPos.containing(pos).pack(), k -> new LinkedHashSet<>()).add(pos.immutable());
    }

    public void add(BlockPos pos) {
        if (put(pos)) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        long chunk = ChunkPos.containing(pos).pack();
        Set<BlockPos> positions = chunks.get(chunk);
        if (positions != null && positions.remove(pos)) {
            if (positions.isEmpty()) {
                chunks.remove(chunk);
            }
            setDirty();
        }
    }

    /** How many polluted blocks the chunk holds, as recorded. */
    public int count(ChunkPos chunk) {
        return chunks.getOrDefault(chunk.pack(), Set.of()).size();
    }

    /** Polluted positions inside {@code box}, in no particular order. */
    public List<BlockPos> within(AABB box) {
        List<BlockPos> found = new ArrayList<>();
        int minX = (int) Math.floor(box.minX) >> 4;
        int maxX = (int) Math.floor(box.maxX) >> 4;
        int minZ = (int) Math.floor(box.minZ) >> 4;
        int maxZ = (int) Math.floor(box.maxZ) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                for (BlockPos pos : chunks.getOrDefault(new ChunkPos(cx, cz).pack(), Set.of())) {
                    if (box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                        found.add(pos);
                    }
                }
            }
        }
        return found;
    }
}
