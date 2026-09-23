package one.nxeu.thaumory.flux.pollution;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * One file in {@code data/<namespace>/thaumory/pollution/}: the polluted block, what turns into it
 * (block ids, or block tags with a leading {@code #}), and what purification turns it back into.
 *
 * <pre>{@code
 * { "polluted": "thaumory:polluted_soil", "from": ["#minecraft:dirt", "#minecraft:grass_blocks"], "restore": "minecraft:dirt" }
 * }</pre>
 */
public record PollutionFile(Identifier polluted, List<String> from, Identifier restore) {
    public static final Codec<PollutionFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("polluted").forGetter(PollutionFile::polluted),
            Codec.STRING.listOf().fieldOf("from").forGetter(PollutionFile::from),
            Identifier.CODEC.fieldOf("restore").forGetter(PollutionFile::restore)
    ).apply(i, PollutionFile::new));
}
