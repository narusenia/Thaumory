package one.nxeu.thaumory.wand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * {@code data/<ns>/thaumory/wand_part/*.json}: an item that goes into a wand (requirements §7.1),
 * either as its caps or as its core, never both.
 *
 * <pre>{@code
 * {"item": "thaumory:gold_wand_cap", "cap": {"essentia": 16}}
 * {"item": "minecraft:stick", "core": {"power": 1.0, "incorrect_for": "minecraft:incorrect_for_stone_tool"}}
 * }</pre>
 */
public record WandPart(Identifier item, Optional<Cap> cap, Optional<Core> core) {
    /** How much of each aspect a wand with these caps holds for its focus. */
    public record Cap(int essentia) {
        public static final Codec<Cap> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("essentia").forGetter(Cap::essentia)
        ).apply(i, Cap::new));
    }

    /**
     * How strongly a wand with this core casts, and what its digging focus cannot break: a block
     * tag, as a tool material names it.
     */
    public record Core(double power, Identifier incorrectFor) {
        public static final Codec<Core> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, Double.MAX_VALUE).fieldOf("power").forGetter(Core::power),
                Identifier.CODEC.fieldOf("incorrect_for").forGetter(Core::incorrectFor)
        ).apply(i, Core::new));
    }

    public static final Codec<WandPart> CODEC = RecordCodecBuilder.<WandPart>create(i -> i.group(
            Identifier.CODEC.fieldOf("item").forGetter(WandPart::item),
            Cap.CODEC.optionalFieldOf("cap").forGetter(WandPart::cap),
            Core.CODEC.optionalFieldOf("core").forGetter(WandPart::core)
    ).apply(i, WandPart::new)).validate(part -> part.cap().isPresent() != part.core().isPresent() ? DataResult.success(part)
            : DataResult.error(() -> "A wand part is either a cap or a core: " + part.item()));
}
