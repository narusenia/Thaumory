package one.nxeu.thaumory.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * A fragment of lore, from {@code data/<namespace>/thaumory/research/hint/<path>.json}, that appears
 * in the book once its conditions hold. Its text is the translation key {@code hint.<namespace>.<path>}.
 */
public record Hint(List<ResearchCondition> conditions) {
    public static final Codec<Hint> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResearchCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Hint::conditions)
    ).apply(i, Hint::new));

    public boolean met(ResearchFacts facts) {
        return conditions.stream().allMatch(condition -> condition.progress(facts).met());
    }

    public static String textKey(Identifier id) {
        return id.toLanguageKey("hint");
    }
}
