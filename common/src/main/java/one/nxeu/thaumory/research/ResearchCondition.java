package one.nxeu.thaumory.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * Something a chapter or hint waits for (requirements §6.2). Each reports how far along the player
 * is, so the book can show "2 / 3".
 */
public sealed interface ResearchCondition {
    Codec<ResearchCondition> CODEC = Codec.STRING.partialDispatch("type",
            condition -> DataResult.success(condition.type()), ResearchCondition::codec);

    /** How far along the player is; met once {@code current} reaches {@code needed}. */
    record Progress(int current, int needed) {
        public boolean met() {
            return current >= needed;
        }
    }

    String type();

    Progress progress(ResearchFacts facts);

    private static DataResult<MapCodec<? extends ResearchCondition>> codec(String type) {
        return switch (type) {
            case Scanned.TYPE -> DataResult.success(Scanned.CODEC);
            case Aspects.TYPE -> DataResult.success(Aspects.CODEC);
            case Circles.TYPE -> DataResult.success(Circles.CODEC);
            default -> DataResult.error(() -> "Unknown research condition: " + type);
        };
    }

    /** One scanned item, any scanned item of a tag, or a number of items scanned. */
    record Scanned(Optional<Identifier> item, Optional<Identifier> tag, Optional<Integer> count) implements ResearchCondition {
        static final String TYPE = "thaumory:scanned";
        static final MapCodec<Scanned> CODEC = RecordCodecBuilder.<Scanned>mapCodec(i -> i.group(
                Identifier.CODEC.optionalFieldOf("item").forGetter(Scanned::item),
                Identifier.CODEC.optionalFieldOf("tag").forGetter(Scanned::tag),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count").forGetter(Scanned::count)
        ).apply(i, Scanned::new)).validate(scanned -> (scanned.item.isPresent() ? 1 : 0) + (scanned.tag.isPresent() ? 1 : 0)
                + (scanned.count.isPresent() ? 1 : 0) == 1
                ? DataResult.success(scanned)
                : DataResult.error(() -> "thaumory:scanned needs exactly one of item, tag or count"));

        public static Scanned item(Identifier item) {
            return new Scanned(Optional.of(item), Optional.empty(), Optional.empty());
        }

        public static Scanned tag(Identifier tag) {
            return new Scanned(Optional.empty(), Optional.of(tag), Optional.empty());
        }

        public static Scanned count(int count) {
            return new Scanned(Optional.empty(), Optional.empty(), Optional.of(count));
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Progress progress(ResearchFacts facts) {
            if (item.isPresent()) {
                return new Progress(facts.scannedItems().contains(item.get()) ? 1 : 0, 1);
            }
            if (tag.isPresent()) {
                return new Progress(facts.scannedAnyIn(tag.get()) ? 1 : 0, 1);
            }
            return new Progress(Math.min(facts.scannedItems().size(), count.orElseThrow()), count.orElseThrow());
        }
    }

    /** A number of aspects worked out, or all of the listed ones. */
    record Aspects(Optional<Integer> count, List<Identifier> aspects) implements ResearchCondition {
        static final String TYPE = "thaumory:aspects";
        static final MapCodec<Aspects> CODEC = RecordCodecBuilder.<Aspects>mapCodec(i -> i.group(
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count").forGetter(Aspects::count),
                Identifier.CODEC.listOf().optionalFieldOf("aspects", List.of()).forGetter(Aspects::aspects)
        ).apply(i, Aspects::new)).validate(aspects -> aspects.count.isPresent() != !aspects.aspects.isEmpty()
                ? DataResult.success(aspects)
                : DataResult.error(() -> "thaumory:aspects needs either count or aspects"));

        public static Aspects count(int count) {
            return new Aspects(Optional.of(count), List.of());
        }

        public static Aspects all(List<Identifier> aspects) {
            return new Aspects(Optional.empty(), List.copyOf(aspects));
        }

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Progress progress(ResearchFacts facts) {
            if (count.isPresent()) {
                return new Progress(Math.min(facts.knownAspects().size(), count.get()), count.get());
            }
            int known = (int) aspects.stream().filter(facts.knownAspects()::contains).count();
            return new Progress(known, aspects.size());
        }
    }

    /** A number of circle combinations recorded as succeeded (or failed), optionally of one effect. */
    record Circles(boolean success, int count, Optional<Identifier> effect) implements ResearchCondition {
        static final String TYPE = "thaumory:circles";
        private static final Codec<Boolean> OUTCOME = Codec.STRING.comapFlatMap(
                name -> switch (name) {
                    case "success" -> DataResult.success(true);
                    case "failure" -> DataResult.success(false);
                    default -> DataResult.error(() -> "Circle outcome must be success or failure: " + name);
                },
                success -> success ? "success" : "failure");
        static final MapCodec<Circles> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                OUTCOME.fieldOf("outcome").forGetter(Circles::success),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(Circles::count),
                Identifier.CODEC.optionalFieldOf("effect").forGetter(Circles::effect)
        ).apply(i, Circles::new));

        @Override
        public String type() {
            return TYPE;
        }

        @Override
        public Progress progress(ResearchFacts facts) {
            return new Progress(Math.min(facts.circles(success, effect), count), count);
        }
    }
}
