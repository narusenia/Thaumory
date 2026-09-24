package one.nxeu.thaumory.fabric.datagen;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ORDO;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TEMPESTAS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VINCULUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VITA;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.research.Chapter;
import one.nxeu.thaumory.research.Hint;
import one.nxeu.thaumory.research.ResearchCondition;

/** The built-in chapters and hints (requirements §6.2). */
final class ResearchProvider {
    static final Identifier BEGINNING = Thaumory.id("beginning");
    static final Identifier ASPECTS = Thaumory.id("aspects");
    static final Identifier CRUCIBLE = Thaumory.id("crucible");
    static final Identifier RUNES = Thaumory.id("runes");
    static final Identifier CIRCLES = Thaumory.id("circles");
    static final Identifier FLUX = Thaumory.id("flux");
    static final Identifier INQUIRY = Thaumory.id("inquiry");

    private ResearchProvider() {}

    /** Writes {@code data/thaumory/thaumory/research/chapter/*.json}. */
    static final class Chapters extends FabricCodecDataProvider<Chapter> {
        Chapters(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/research/chapter", Chapter.CODEC);
        }

        @Override
        protected void configure(BiConsumer<Identifier, Chapter> output, HolderLookup.Provider registries) {
            output.accept(BEGINNING, chapter(ThaumoryItems.ARCANE_CODEX.get(), 0, List.of(), List.of(), List.of()));
            output.accept(ASPECTS, chapter(ThaumoryItems.ARCANE_LOUPE.get(), 1, List.of(BEGINNING),
                    List.of(ResearchCondition.Aspects.count(1)), List.of()));
            output.accept(CRUCIBLE, chapter(ThaumoryItems.CRUCIBLE.get(), 2, List.of(BEGINNING),
                    List.of(ResearchCondition.Scanned.item(key(ThaumoryItems.CRUCIBLE.get()))), List.of()));
            output.accept(RUNES, chapter(ThaumoryItems.BLANK_RUNE.get(), 3, List.of(ASPECTS, CRUCIBLE),
                    List.of(ResearchCondition.Aspects.count(3)), List.of(alchemy("blank_rune"), alchemy("chalk"))));
            output.accept(CIRCLES, chapter(ThaumoryItems.CIRCLE_CORE.get(), 4, List.of(RUNES),
                    List.of(new ResearchCondition.Circles(true, 1, Optional.empty())),
                    List.of(alchemy("amplifying_chalk"), alchemy("extending_chalk"), alchemy("economizing_chalk"), alchemy("stabilizing_chalk"))));
            output.accept(FLUX, chapter(ThaumoryItems.POLLUTED_SOIL.get(), 5, List.of(CIRCLES),
                    List.of(new ResearchCondition.Circles(false, 1, Optional.empty())), List.of()));
            output.accept(INQUIRY, chapter(ThaumoryItems.WAND.get(), 6, List.of(ASPECTS),
                    List.of(ResearchCondition.Aspects.count(12)), List.of()));
        }

        private static Chapter chapter(ItemLike icon, int order, List<Identifier> requires, List<ResearchCondition> conditions,
                List<Identifier> unlocks) {
            return new Chapter(key(icon), order, requires, conditions, unlocks);
        }

        private static Identifier key(ItemLike item) {
            return BuiltInRegistries.ITEM.getKey(item.asItem());
        }

        private static Identifier alchemy(String name) {
            return Thaumory.id("alchemy/" + name);
        }

        @Override
        public String getName() {
            return "Thaumory research chapters";
        }
    }

    /** Writes {@code data/thaumory/thaumory/research/hint/*.json}: one per built-in circle effect, named after it. */
    static final class Hints extends FabricCodecDataProvider<Hint> {
        Hints(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/research/hint", Hint.CODEC);
        }

        @Override
        protected void configure(BiConsumer<Identifier, Hint> output, HolderLookup.Provider registries) {
            hint(output, ThaumoryCircleEffects.TELEPORT, ARCANUM, AER);
            hint(output, ThaumoryCircleEffects.LIGHT, LUX, IGNIS);
            hint(output, ThaumoryCircleEffects.PURIFICATION, ORDO, LUX);
            hint(output, ThaumoryCircleEffects.WARD, VINCULUM, ORDO);
            hint(output, ThaumoryCircleEffects.GROWTH, HERBA, VITA);
            hint(output, ThaumoryCircleEffects.HEALING, VITA, ORDO);
            hint(output, ThaumoryCircleEffects.ATTRACTION, TEMPESTAS, VINCULUM);
            hint(output, ThaumoryCircleEffects.WEATHER, TEMPESTAS, ARCANUM);
        }

        private static void hint(BiConsumer<Identifier, Hint> output, Identifier effect, Aspect first, Aspect second) {
            output.accept(effect, new Hint(List.of(ResearchCondition.Aspects.all(List.of(first.id(), second.id())))));
        }

        @Override
        public String getName() {
            return "Thaumory research hints";
        }
    }
}
