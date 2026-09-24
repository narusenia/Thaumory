package one.nxeu.thaumory.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * A chapter of the book, from {@code data/<namespace>/thaumory/research/chapter/<path>.json}. It
 * opens once every chapter it {@code requires} is complete, and completes once all its
 * {@code conditions} hold, unlocking its alchemy recipes. Its title and text are the translation
 * keys {@code chapter.<namespace>.<path>} and {@code ….text}. The book draws it in its
 * {@code category}'s tree at ({@code x}, {@code y}), or where {@link ChapterLayout} puts it.
 */
public record Chapter(Identifier icon, Identifier category, Optional<Integer> x, Optional<Integer> y, List<Identifier> requires,
        List<ResearchCondition> conditions, List<Identifier> unlocks) {
    public static final Identifier BASICS = Identifier.fromNamespaceAndPath("thaumory", "basics");

    public static final Codec<Chapter> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("icon").forGetter(Chapter::icon),
            Identifier.CODEC.optionalFieldOf("category", BASICS).forGetter(Chapter::category),
            Codec.INT.optionalFieldOf("x").forGetter(Chapter::x),
            Codec.INT.optionalFieldOf("y").forGetter(Chapter::y),
            Identifier.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Chapter::requires),
            ResearchCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Chapter::conditions),
            Identifier.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(Chapter::unlocks)
    ).apply(i, Chapter::new));

    /** The cell the file asks for; both coordinates are needed. */
    public Optional<ChapterLayout.Cell> position() {
        return x.flatMap(column -> y.map(row -> new ChapterLayout.Cell(column, row)));
    }

    public boolean met(ResearchFacts facts) {
        return conditions.stream().allMatch(condition -> condition.progress(facts).met());
    }

    public static String titleKey(Identifier id) {
        return id.toLanguageKey("chapter");
    }

    public static String textKey(Identifier id) {
        return id.toLanguageKey("chapter") + ".text";
    }
}
