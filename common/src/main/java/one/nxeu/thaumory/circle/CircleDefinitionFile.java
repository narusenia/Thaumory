package one.nxeu.thaumory.circle;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * One file under {@code data/<namespace>/thaumory/circle/}: which effect a pair of runes starts,
 * which slot 3 runes it accepts, and what it costs.
 *
 * <pre>{@code
 * {
 *   "effect": "thaumory:light",
 *   "runes": ["thaumory:lux", "thaumory:ignis"],
 *   "slot3": ["none", "thaumory:umbra"],
 *   "mode": "sustained",
 *   "interval": 200
 * }
 * }</pre>
 *
 * {@code slot3} is either a list ({@code "none"} for an empty slot) or {@code "any"}: any aspect,
 * but not empty. A triggered effect takes {@code cost}, a sustained one {@code interval}.
 *
 * @param slot3 empty for {@code "any"}; otherwise the accepted parameters, empty meaning no rune
 */
public record CircleDefinitionFile(Identifier effect, List<Identifier> runes, Optional<List<Optional<Identifier>>> slot3,
        CircleMode mode, int cost, int interval) {
    public static final String ANY = "any";
    public static final String NONE = "none";

    private static final Codec<Optional<Identifier>> PARAMETER = Codec.STRING.comapFlatMap(
            name -> name.equals(NONE) ? DataResult.success(Optional.empty()) : Identifier.read(name).map(Optional::of),
            parameter -> parameter.map(Identifier::toString).orElse(NONE));

    private static final Codec<Optional<List<Optional<Identifier>>>> SLOT3 = Codec.either(
            Codec.STRING.comapFlatMap(
                    name -> name.equals(ANY) ? DataResult.success(ANY) : DataResult.error(() -> "Expected \"any\" or a list, got " + name),
                    name -> name),
            PARAMETER.listOf()
    ).xmap(either -> either.right(), list -> list.<Either<String, List<Optional<Identifier>>>>map(Either::right)
            .orElseGet(() -> Either.left(ANY)));

    public static final Codec<CircleDefinitionFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("effect").forGetter(CircleDefinitionFile::effect),
            Identifier.CODEC.listOf(2, 2).fieldOf("runes").forGetter(CircleDefinitionFile::runes),
            SLOT3.optionalFieldOf("slot3", Optional.of(List.of(Optional.empty()))).forGetter(CircleDefinitionFile::slot3),
            CircleMode.CODEC.fieldOf("mode").forGetter(CircleDefinitionFile::mode),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("cost", 1).forGetter(CircleDefinitionFile::cost),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("interval", 200).forGetter(CircleDefinitionFile::interval)
    ).apply(i, CircleDefinitionFile::new));
}
