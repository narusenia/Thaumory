package one.nxeu.thaumory.circle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.knowledge.CircleCombination;

/**
 * Where each working circle in a dimension is, by the runes it holds, saved with the world. Lets
 * a teleport circle find its partners even in chunks that are not loaded. Cores keep it up to
 * date; an entry can still be stale, so whoever reads it checks the Core that is really there.
 */
public final class CircleIndex extends SavedData {
    private record Entry(CircleCombination combination, List<BlockPos> positions) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                CircleCombination.CODEC.fieldOf("combination").forGetter(Entry::combination),
                BlockPos.CODEC.listOf().fieldOf("positions").forGetter(Entry::positions)
        ).apply(i, Entry::new));
    }

    private static final Codec<CircleIndex> CODEC = Entry.CODEC.listOf().xmap(entries -> {
        CircleIndex index = new CircleIndex();
        entries.forEach(entry -> index.circles.put(entry.combination(), new LinkedHashSet<>(entry.positions())));
        return index;
    }, index -> index.circles.entrySet().stream().map(e -> new Entry(e.getKey(), List.copyOf(e.getValue()))).toList());

    private static final SavedDataType<CircleIndex> TYPE = new SavedDataType<>(Thaumory.id("circles"), CircleIndex::new, CODEC, DataFixTypes.LEVEL);

    private final Map<CircleCombination, Set<BlockPos>> circles = new HashMap<>();

    public static CircleIndex of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Records that the Core at {@code pos} now makes {@code combination}, and nothing else. */
    public void put(BlockPos pos, CircleCombination combination) {
        if (circles.getOrDefault(combination, Set.of()).contains(pos)) {
            return;
        }
        remove(pos);
        circles.computeIfAbsent(combination, k -> new LinkedHashSet<>()).add(pos.immutable());
        setDirty();
    }

    public void remove(BlockPos pos) {
        boolean changed = false;
        for (var iterator = circles.values().iterator(); iterator.hasNext(); ) {
            Set<BlockPos> positions = iterator.next();
            changed |= positions.remove(pos);
            if (positions.isEmpty()) {
                iterator.remove();
            }
        }
        if (changed) {
            setDirty();
        }
    }

    /** Other circles making {@code combination}, nearest to {@code from} first. */
    public List<BlockPos> nearest(CircleCombination combination, BlockPos from) {
        List<BlockPos> found = new ArrayList<>(circles.getOrDefault(combination, Set.of()));
        found.remove(from);
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(from)));
        return found;
    }
}
