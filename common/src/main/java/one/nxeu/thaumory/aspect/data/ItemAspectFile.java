package one.nxeu.thaumory.aspect.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * One {@code data/<namespace>/thaumory/item_aspects/*.json} file.
 *
 * <pre>{@code
 * {
 *   "values": [
 *     { "target": "minecraft:oak_log", "aspects": { "thaumory:herba": 4, "thaumory:terra": 1 } },
 *     { "target": "#minecraft:wool", "aspects": { "thaumory:bestia": 2 } }
 *   ]
 * }
 * }</pre>
 *
 * An empty {@code aspects} object means the target explicitly has no aspects.
 */
public record ItemAspectFile(List<Entry> values) {
    public static final Codec<ItemAspectFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Entry.CODEC.listOf().fieldOf("values").forGetter(ItemAspectFile::values)
    ).apply(i, ItemAspectFile::new));

    public record Entry(Target target, Map<Identifier, Integer> aspects) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Target.CODEC.fieldOf("target").forGetter(Entry::target),
                Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, 1_000_000))
                        .fieldOf("aspects").forGetter(Entry::aspects)
        ).apply(i, Entry::new));
    }

    /** An item id, or an item tag id when written with a leading {@code #}. */
    public record Target(Identifier id, boolean tag) {
        public static final Codec<Target> CODEC = Codec.STRING.comapFlatMap(Target::parse, Target::toString);

        public static DataResult<Target> parse(String value) {
            boolean tag = value.startsWith("#");
            return Identifier.read(tag ? value.substring(1) : value).map(id -> new Target(id, tag));
        }

        @Override
        public String toString() {
            return tag ? "#" + id : id.toString();
        }
    }
}
