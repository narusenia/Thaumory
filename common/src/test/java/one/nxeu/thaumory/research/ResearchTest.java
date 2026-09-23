package one.nxeu.thaumory.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class ResearchTest {
    private static final Identifier BEGINNING = id("beginning");
    private static final Identifier ASPECTS = id("aspects");
    private static final Identifier CRUCIBLE = id("crucible");
    private static final Identifier RUNES = id("runes");
    private static final Identifier BLANK_RUNE = id("blank_rune");
    private static final Identifier CRUCIBLE_ITEM = id("crucible");

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("thaumory", path);
    }

    private static final class Facts implements ResearchFacts {
        final Set<Identifier> scanned = new HashSet<>();
        final Set<Identifier> aspects = new HashSet<>();
        final Set<Identifier> taggedItems = new HashSet<>();
        int successes;
        int failures;
        int lightSuccesses;

        @Override
        public Set<Identifier> scannedItems() {
            return scanned;
        }

        @Override
        public boolean scannedAnyIn(Identifier tag) {
            return scanned.stream().anyMatch(taggedItems::contains);
        }

        @Override
        public Set<Identifier> knownAspects() {
            return aspects;
        }

        @Override
        public int circles(boolean success, Optional<Identifier> effect) {
            if (effect.isPresent()) {
                return success ? lightSuccesses : 0;
            }
            return success ? successes : failures;
        }
    }

    private static Chapter chapter(int order, List<Identifier> requires, List<ResearchCondition> conditions, List<Identifier> unlocks) {
        return new Chapter(Identifier.withDefaultNamespace("book"), order, requires, conditions, unlocks);
    }

    private static Research tree() {
        return new Research(Map.of(
                BEGINNING, chapter(0, List.of(), List.of(), List.of()),
                ASPECTS, chapter(1, List.of(BEGINNING), List.of(ResearchCondition.Aspects.count(1)), List.of()),
                CRUCIBLE, chapter(2, List.of(BEGINNING), List.of(ResearchCondition.Scanned.item(CRUCIBLE_ITEM)), List.of()),
                RUNES, chapter(3, List.of(ASPECTS, CRUCIBLE), List.of(ResearchCondition.Aspects.count(3)), List.of(BLANK_RUNE))),
                Map.of(id("teleport"), new Hint(List.of(ResearchCondition.Aspects.all(List.of(id("arcanum"), id("aer")))))));
    }

    @Test
    void chaptersWithoutConditionsCompleteAsSoonAsTheyOpen() {
        Research.Advance advance = tree().advance(new Facts(), Set.of(), Set.of());
        assertEquals(List.of(BEGINNING), advance.chapters());
    }

    @Test
    void completingOneChapterCanCompleteTheNextRightAway() {
        Facts facts = new Facts();
        facts.aspects.addAll(Set.of(id("ignis"), id("aer"), id("aqua")));
        facts.scanned.add(CRUCIBLE_ITEM);
        assertEquals(List.of(BEGINNING, ASPECTS, CRUCIBLE, RUNES), tree().advance(facts, Set.of(), Set.of()).chapters());
    }

    @Test
    void closedChaptersStayClosedEvenWhenTheirConditionsHold() {
        Facts facts = new Facts();
        facts.aspects.addAll(Set.of(id("ignis"), id("aer"), id("aqua")));
        // No crucible scanned, so the runes chapter never opens.
        Research.Advance advance = tree().advance(facts, Set.of(), Set.of());
        assertEquals(List.of(BEGINNING, ASPECTS), advance.chapters());
        assertEquals(1, tree().closedCount(Set.of(BEGINNING, ASPECTS)));
        assertEquals(List.of(BEGINNING, ASPECTS, CRUCIBLE), tree().visible(Set.of(BEGINNING, ASPECTS)));
    }

    @Test
    void hintsAppearOnceAndOnlyWhenTheyHold() {
        Facts facts = new Facts();
        facts.aspects.add(id("arcanum"));
        assertTrue(tree().advance(facts, Set.of(), Set.of()).hints().isEmpty());
        facts.aspects.add(id("aer"));
        assertEquals(List.of(id("teleport")), tree().advance(facts, Set.of(), Set.of()).hints());
        assertTrue(tree().advance(facts, Set.of(), Set.of(id("teleport"))).hints().isEmpty());
    }

    @Test
    void recipesNoChapterUnlocksAreAlwaysUsable() {
        assertTrue(tree().canUse(id("chalk"), Set.of()));
        assertFalse(tree().canUse(BLANK_RUNE, Set.of(BEGINNING)));
        assertTrue(tree().canUse(BLANK_RUNE, Set.of(RUNES)));
    }

    @Test
    void conditionsReportProgress() {
        Facts facts = new Facts();
        facts.aspects.add(id("ignis"));
        facts.scanned.addAll(Set.of(id("a"), id("b")));
        facts.taggedItems.add(id("b"));
        facts.failures = 2;
        facts.lightSuccesses = 1;
        assertEquals(new ResearchCondition.Progress(1, 3), ResearchCondition.Aspects.count(3).progress(facts));
        assertEquals(new ResearchCondition.Progress(0, 2),
                ResearchCondition.Aspects.all(List.of(id("lux"), id("umbra"))).progress(facts));
        assertEquals(new ResearchCondition.Progress(2, 2), ResearchCondition.Scanned.count(2).progress(facts));
        assertTrue(ResearchCondition.Scanned.tag(id("tagged")).progress(facts).met());
        assertTrue(new ResearchCondition.Circles(false, 1, Optional.empty()).progress(facts).met());
        assertFalse(new ResearchCondition.Circles(true, 1, Optional.empty()).progress(facts).met());
        assertTrue(new ResearchCondition.Circles(true, 1, Optional.of(id("light"))).progress(facts).met());
    }

    @Test
    void chapterFilesParseAndRejectAmbiguousConditions() {
        Chapter chapter = Chapter.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"icon": "minecraft:cauldron", "order": 3, "requires": ["thaumory:beginning"],
                 "conditions": [{"type": "thaumory:scanned", "item": "thaumory:crucible"},
                                {"type": "thaumory:circles", "outcome": "failure"}],
                 "unlocks": ["thaumory:blank_rune"]}""")).getOrThrow();
        assertEquals(3, chapter.order());
        assertEquals(List.of(ResearchCondition.Scanned.item(CRUCIBLE_ITEM), new ResearchCondition.Circles(false, 1, Optional.empty())),
                chapter.conditions());
        assertTrue(Chapter.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"icon": "minecraft:cauldron", "conditions": [{"type": "thaumory:scanned", "item": "a:b", "count": 2}]}""")).isError());
        assertTrue(Chapter.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"icon": "minecraft:cauldron", "conditions": [{"type": "thaumory:nonsense"}]}""")).isError());
    }
}
