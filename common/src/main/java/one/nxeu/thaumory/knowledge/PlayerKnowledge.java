package one.nxeu.thaumory.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * What one player has learned. Immutable; every {@code with...} method returns this same instance
 * when nothing changes, so callers can skip saving and syncing.
 *
 * <p>Everything is kept by id, including ids that no longer exist (a removed mod), so the
 * knowledge comes back if the content does.
 *
 * @param scanned  scanned ids per registry, e.g. {@code minecraft:item}. Blocks count as their item
 * @param aspects  aspects whose names the player has worked out
 * @param circles  every combination tried in a Core. A success is never overwritten by a failure
 * @param chapters book chapters the player has completed
 * @param hints    hints that have appeared in the book
 */
public record PlayerKnowledge(
        Map<Identifier, Set<Identifier>> scanned,
        Set<Identifier> aspects,
        Map<CircleCombination, CircleOutcome> circles,
        Set<Identifier> chapters,
        Set<Identifier> hints) {
    public static final Identifier ITEMS = Identifier.withDefaultNamespace("item");
    public static final PlayerKnowledge EMPTY = new PlayerKnowledge(Map.of(), Set.of(), Map.of(), Set.of(), Set.of());

    private static final Codec<Set<Identifier>> ID_SET = Identifier.CODEC.listOf()
            .xmap(Set::copyOf, ids -> ids.stream().sorted().toList());

    private record CircleEntry(CircleCombination combination, CircleOutcome outcome) {
        static final Codec<CircleEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                CircleCombination.CODEC.fieldOf("combination").forGetter(CircleEntry::combination),
                CircleOutcome.CODEC.fieldOf("outcome").forGetter(CircleEntry::outcome)
        ).apply(i, CircleEntry::new));
    }

    private static final Codec<Map<CircleCombination, CircleOutcome>> CIRCLES = CircleEntry.CODEC.listOf().xmap(
            entries -> {
                Map<CircleCombination, CircleOutcome> circles = new LinkedHashMap<>();
                entries.forEach(entry -> circles.merge(entry.combination(), entry.outcome(), CircleOutcome::best));
                return circles;
            },
            circles -> circles.entrySet().stream()
                    .map(entry -> new CircleEntry(entry.getKey(), entry.getValue()))
                    .sorted((a, b) -> a.combination().toString().compareTo(b.combination().toString()))
                    .toList());

    /** Saved with the player. Every field is optional so older saves keep loading. */
    public static final Codec<PlayerKnowledge> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Identifier.CODEC, ID_SET).optionalFieldOf("scanned", Map.of()).forGetter(PlayerKnowledge::scanned),
            ID_SET.optionalFieldOf("aspects", Set.of()).forGetter(PlayerKnowledge::aspects),
            CIRCLES.optionalFieldOf("circles", Map.of()).forGetter(PlayerKnowledge::circles),
            ID_SET.optionalFieldOf("chapters", Set.of()).forGetter(PlayerKnowledge::chapters),
            ID_SET.optionalFieldOf("hints", Set.of()).forGetter(PlayerKnowledge::hints)
    ).apply(i, PlayerKnowledge::new));

    private static final StreamCodec<ByteBuf, Set<Identifier>> ID_SET_STREAM =
            ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC);

    public static final StreamCodec<ByteBuf, PlayerKnowledge> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ID_SET_STREAM), PlayerKnowledge::scanned,
            ID_SET_STREAM, PlayerKnowledge::aspects,
            ByteBufCodecs.map(HashMap::new, CircleCombination.STREAM_CODEC, CircleOutcome.STREAM_CODEC), PlayerKnowledge::circles,
            ID_SET_STREAM, PlayerKnowledge::chapters,
            ID_SET_STREAM, PlayerKnowledge::hints,
            PlayerKnowledge::new);

    public PlayerKnowledge {
        Map<Identifier, Set<Identifier>> copied = new HashMap<>();
        scanned.forEach((registry, ids) -> {
            if (!ids.isEmpty()) {
                copied.put(registry, Set.copyOf(ids));
            }
        });
        scanned = Map.copyOf(copied);
        aspects = Set.copyOf(aspects);
        circles = Map.copyOf(circles);
        chapters = Set.copyOf(chapters);
        hints = Set.copyOf(hints);
    }

    public Set<Identifier> scanned(Identifier registry) {
        return scanned.getOrDefault(registry, Set.of());
    }

    public boolean hasScanned(Identifier registry, Identifier id) {
        return scanned(registry).contains(id);
    }

    public PlayerKnowledge withScanned(Identifier registry, Identifier id) {
        if (hasScanned(registry, id)) {
            return this;
        }
        Map<Identifier, Set<Identifier>> newScanned = new HashMap<>(scanned);
        newScanned.put(registry, added(scanned(registry), id));
        return new PlayerKnowledge(newScanned, aspects, circles, chapters, hints);
    }

    public boolean knowsAspect(Identifier aspect) {
        return aspects.contains(aspect);
    }

    public PlayerKnowledge withAspect(Identifier aspect) {
        return knowsAspect(aspect) ? this : new PlayerKnowledge(scanned, added(aspects, aspect), circles, chapters, hints);
    }

    public Optional<CircleOutcome> circle(CircleCombination combination) {
        return Optional.ofNullable(circles.get(combination));
    }

    public PlayerKnowledge withCircle(CircleCombination combination, CircleOutcome outcome) {
        CircleOutcome recorded = circle(combination).map(previous -> previous.best(outcome)).orElse(outcome);
        if (recorded == circles.get(combination)) {
            return this;
        }
        Map<CircleCombination, CircleOutcome> newCircles = new HashMap<>(circles);
        newCircles.put(combination, recorded);
        return new PlayerKnowledge(scanned, aspects, newCircles, chapters, hints);
    }

    public boolean hasCompleted(Identifier chapter) {
        return chapters.contains(chapter);
    }

    public PlayerKnowledge withChapter(Identifier chapter) {
        return hasCompleted(chapter) ? this : new PlayerKnowledge(scanned, aspects, circles, added(chapters, chapter), hints);
    }

    public boolean hasHint(Identifier hint) {
        return hints.contains(hint);
    }

    public PlayerKnowledge withHint(Identifier hint) {
        return hasHint(hint) ? this : new PlayerKnowledge(scanned, aspects, circles, chapters, added(hints, hint));
    }

    private static Set<Identifier> added(Set<Identifier> ids, Identifier id) {
        Set<Identifier> result = new HashSet<>(ids);
        result.add(id);
        return result;
    }

    public enum CircleOutcome implements StringRepresentable {
        SUCCESS("success"),
        FAILURE("failure");

        public static final Codec<CircleOutcome> CODEC = StringRepresentable.fromEnum(CircleOutcome::values);
        public static final StreamCodec<ByteBuf, CircleOutcome> STREAM_CODEC = ByteBufCodecs.BOOL.map(
                success -> success ? SUCCESS : FAILURE, outcome -> outcome == SUCCESS);

        private final String name;

        CircleOutcome(String name) {
            this.name = name;
        }

        /** A success, once seen, is what the book keeps. */
        CircleOutcome best(CircleOutcome other) {
            return this == SUCCESS || other == SUCCESS ? SUCCESS : FAILURE;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
