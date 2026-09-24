package one.nxeu.thaumory.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

/**
 * A field of the book's chapters, from {@code data/<namespace>/thaumory/research/category/<path>.json}:
 * one tree of its own, picked with a bookmark showing {@code icon}, in {@code order}, and drawn over
 * tiles of {@code background} ({@code textures/<path>.png}). Its name is the translation key
 * {@code category.<namespace>.<path>}.
 */
public record Category(Identifier icon, int order, Identifier background) {
    public static final Identifier DEFAULT_BACKGROUND = Identifier.withDefaultNamespace("block/stone");

    public static final Codec<Category> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("icon").forGetter(Category::icon),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Category::order),
            Identifier.CODEC.optionalFieldOf("background", DEFAULT_BACKGROUND).forGetter(Category::background)
    ).apply(i, Category::new));

    public static String nameKey(Identifier id) {
        return id.toLanguageKey("category");
    }
}
