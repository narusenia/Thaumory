package one.nxeu.thaumory.research;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * The chapters, hints and categories of the loaded datapacks, from
 * {@code data/<namespace>/thaumory/research/chapter/}, {@code …/hint/} and {@code …/category/}. Players catch up with changes on their next research check.
 */
public final class ResearchData {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Map<Identifier, Chapter> chapters = Map.of();
    private static volatile Map<Identifier, Hint> hints = Map.of();
    private static volatile Map<Identifier, Category> categories = Map.of();
    private static volatile Research research = Research.EMPTY;

    private ResearchData() {}

    public static Research research() {
        return research;
    }

    public static SimpleJsonResourceReloadListener<Chapter> chapterListener() {
        return new Listener<>(Chapter.CODEC, "chapter", loaded -> {
            chapters = Map.copyOf(loaded);
            rebuild();
        });
    }

    public static SimpleJsonResourceReloadListener<Hint> hintListener() {
        return new Listener<>(Hint.CODEC, "hint", loaded -> {
            hints = Map.copyOf(loaded);
            rebuild();
        });
    }

    public static SimpleJsonResourceReloadListener<Category> categoryListener() {
        return new Listener<>(Category.CODEC, "category", loaded -> {
            categories = Map.copyOf(loaded);
            rebuild();
        });
    }

    private static void rebuild() {
        research = new Research(chapters, hints, categories);
    }

    private static final class Listener<T> extends SimpleJsonResourceReloadListener<T> {
        private final String kind;
        private final Consumer<Map<Identifier, T>> apply;

        Listener(Codec<T> codec, String kind, Consumer<Map<Identifier, T>> apply) {
            super(codec, FileToIdConverter.json("thaumory/research/" + kind));
            this.kind = kind;
            this.apply = apply;
        }

        @Override
        protected void apply(Map<Identifier, T> loaded, ResourceManager manager, ProfilerFiller profiler) {
            apply.accept(loaded);
            LOGGER.info("Loaded {} research {}s", loaded.size(), kind);
        }
    }
}
