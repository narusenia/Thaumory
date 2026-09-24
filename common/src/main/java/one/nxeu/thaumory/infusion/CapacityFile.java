package one.nxeu.thaumory.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;

/**
 * One {@code data/<namespace>/thaumory/infusion_capacity/*.json} file (requirements §10.2).
 *
 * <pre>{@code
 * {
 *   "values": [
 *     { "target": "#thaumory:arcane_iron_equipment", "capacity": 3 },
 *     { "target": "thaumory:arcane_iron_hoe", "capacity": 0 }
 *   ]
 * }
 * }</pre>
 *
 * A capacity of 0 means the target takes no infusions at all.
 */
public record CapacityFile(List<Entry> values) {
    public static final Codec<CapacityFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().fieldOf("values").forGetter(CapacityFile::values)
    ).apply(i, CapacityFile::new));

    public record Entry(ItemAspectFile.Target target, int capacity) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemAspectFile.Target.CODEC.fieldOf("target").forGetter(Entry::target),
                Codec.intRange(0, 1_000).fieldOf("capacity").forGetter(Entry::capacity)
        ).apply(i, Entry::new));
    }
}
