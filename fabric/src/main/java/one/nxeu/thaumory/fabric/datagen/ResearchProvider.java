package one.nxeu.thaumory.fabric.datagen;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.ABYSSUS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AER;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ANIMA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AQUA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARCANUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ARTIFICIUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.AURORA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BELLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.BESTIA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.CAELUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.CHAOS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.FONS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.HERBA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.IGNIS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.LUX;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.METALLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.MORS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.ORDO;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.PEREGRINUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.SIGILLUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.SOLUTIO;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.SORDES;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TARTARUS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TEMPESTAS;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.TERRA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.UMBRA;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VENENUM;
import static one.nxeu.thaumory.aspect.ThaumoryAspects.VESPER;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.research.Chapter;
import one.nxeu.thaumory.research.Category;
import one.nxeu.thaumory.research.Hint;
import one.nxeu.thaumory.research.ResearchCondition;

/** The built-in chapters, categories and hints (requirements §6.2). */
final class ResearchProvider {
    static final Identifier BEGINNING = Thaumory.id("beginning");
    static final Identifier ASPECTS = Thaumory.id("aspects");
    static final Identifier CRUCIBLE = Thaumory.id("crucible");
    static final Identifier RUNES = Thaumory.id("runes");
    static final Identifier CIRCLES = Thaumory.id("circles");
    static final Identifier FLUX = Thaumory.id("flux");
    static final Identifier INQUIRY = Thaumory.id("inquiry");
    static final Identifier ARCANE_METALS = Thaumory.id("arcane_metals");
    static final Identifier AETHER_SILVER = Thaumory.id("aether_silver");
    static final Identifier MONOCLE = Thaumory.id("monocle");

    static final Identifier BASICS = Chapter.BASICS;
    static final Identifier ALCHEMY = Thaumory.id("alchemy");
    static final Identifier CIRCLES_CATEGORY = Thaumory.id("circles");
    static final Identifier METALS = Thaumory.id("metals");

    private ResearchProvider() {}

    /** Writes {@code data/thaumory/thaumory/research/chapter/*.json}. */
    static final class Chapters extends FabricCodecDataProvider<Chapter> {
        Chapters(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/research/chapter", Chapter.CODEC);
        }

        @Override
        protected void configure(BiConsumer<Identifier, Chapter> output, HolderLookup.Provider registries) {
            output.accept(BEGINNING, chapter(ThaumoryItems.ARCANE_CODEX.get(), BASICS, 0, 0, List.of(), List.of(), List.of()));
            output.accept(ASPECTS, chapter(ThaumoryItems.ARCANE_LOUPE.get(), BASICS, 1, 0, List.of(BEGINNING),
                    List.of(ResearchCondition.Aspects.count(1)), List.of()));
            output.accept(CRUCIBLE, chapter(ThaumoryItems.CRUCIBLE.get(), ALCHEMY, 0, 0, List.of(BEGINNING),
                    List.of(ResearchCondition.Scanned.item(key(ThaumoryItems.CRUCIBLE.get()))), List.of()));
            output.accept(RUNES, chapter(ThaumoryItems.BLANK_RUNE.get(), ALCHEMY, 1, 0, List.of(ASPECTS, CRUCIBLE),
                    List.of(ResearchCondition.Aspects.count(3)), List.of(alchemy("blank_rune"), alchemy("chalk"))));
            output.accept(CIRCLES, chapter(ThaumoryItems.CIRCLE_CORE.get(), CIRCLES_CATEGORY, 0, 0, List.of(RUNES),
                    List.of(new ResearchCondition.Circles(true, 1, Optional.empty())),
                    List.of(alchemy("amplifying_chalk"), alchemy("extending_chalk"), alchemy("economizing_chalk"), alchemy("stabilizing_chalk"),
                            alchemy("blank_scroll"))));
            output.accept(FLUX, chapter(ThaumoryItems.POLLUTED_SOIL.get(), CIRCLES_CATEGORY, 1, 0, List.of(CIRCLES),
                    List.of(new ResearchCondition.Circles(false, 1, Optional.empty())), List.of()));
            output.accept(INQUIRY, chapter(ThaumoryItems.WAND.get(), BASICS, 2, 0, List.of(ASPECTS),
                    List.of(ResearchCondition.Aspects.count(12)), List.of()));
            output.accept(ARCANE_METALS, chapter(ThaumoryItems.ARCANE_IRON.ingot().get(), METALS, 0, 0, List.of(CIRCLES),
                    List.of(ResearchCondition.Scanned.item(key(Items.IRON_INGOT))), List.of(alchemy("arcane_iron_ingot"), alchemy("crystal_wand_core"))));
            output.accept(AETHER_SILVER, chapter(ThaumoryItems.AETHER_SILVER.ingot().get(), METALS, 1, 0, List.of(ARCANE_METALS),
                    List.of(ResearchCondition.Scanned.item(key(ThaumoryItems.ARCANE_IRON.ingot().get())),
                            ResearchCondition.Aspects.all(List.of(AER.id(), LUX.id()))),
                    List.of(alchemy("aether_silver_ingot"))));
            output.accept(MONOCLE, chapter(ThaumoryItems.MONOCLE.get(), BASICS, 2, 1, List.of(ASPECTS, CRUCIBLE),
                    List.of(ResearchCondition.Aspects.all(List.of(LUX.id(), ARCANUM.id()))), List.of(alchemy("monocle"))));
        }

        private static Chapter chapter(ItemLike icon, Identifier category, int x, int y, List<Identifier> requires,
                List<ResearchCondition> conditions, List<Identifier> unlocks) {
            return new Chapter(key(icon), category, Optional.of(x), Optional.of(y), requires, conditions, unlocks);
        }

        static Identifier key(ItemLike item) {
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

    /** Writes {@code data/thaumory/thaumory/research/category/*.json}. */
    static final class Categories extends FabricCodecDataProvider<Category> {
        Categories(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/research/category", Category.CODEC);
        }

        @Override
        protected void configure(BiConsumer<Identifier, Category> output, HolderLookup.Provider registries) {
            output.accept(BASICS, new Category(Chapters.key(ThaumoryItems.ARCANE_CODEX.get()), 0, block("dark_oak_planks")));
            output.accept(ALCHEMY, new Category(Chapters.key(ThaumoryItems.CRUCIBLE.get()), 1, block("polished_blackstone_bricks")));
            output.accept(CIRCLES_CATEGORY, new Category(Chapters.key(ThaumoryItems.CIRCLE_CORE.get()), 2, block("end_stone")));
            output.accept(METALS, new Category(Chapters.key(ThaumoryItems.ARCANE_IRON.ingot().get()), 3, block("iron_block")));
        }

        private static Identifier block(String name) {
            return Identifier.withDefaultNamespace("block/" + name);
        }

        @Override
        public String getName() {
            return "Thaumory research categories";
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
            hint(output, ThaumoryCircleEffects.CHARGING, ARCANUM, VINCULUM);
            hint(output, ThaumoryCircleEffects.HARVEST, HERBA, MORS);
            hint(output, ThaumoryCircleEffects.BREEDING, BESTIA, VITA);
            hint(output, ThaumoryCircleEffects.MOISTURE, AQUA, HERBA);
            hint(output, ThaumoryCircleEffects.SMELTING, IGNIS, METALLUM);
            hint(output, ThaumoryCircleEffects.MINING, BELLUM, TERRA);
            hint(output, ThaumoryCircleEffects.SORTING, ORDO, TEMPESTAS);
            hint(output, ThaumoryCircleEffects.MELTING, IGNIS, CHAOS);
            hint(output, ThaumoryCircleEffects.LIGHTNESS, AER, TERRA);
            hint(output, ThaumoryCircleEffects.BREATH, AQUA, AER);
            hint(output, ThaumoryCircleEffects.NIGHT_SIGHT, LUX, UMBRA);
            hint(output, ThaumoryCircleEffects.SAFEGUARD, ORDO, TERRA);
            hint(output, ThaumoryCircleEffects.LURE, BESTIA, VINCULUM);
            hint(output, ThaumoryCircleEffects.BINDING, UMBRA, VINCULUM);
            hint(output, ThaumoryCircleEffects.SEARING, BELLUM, IGNIS);
            hint(output, ThaumoryCircleEffects.WITHERING, MORS, VENENUM);
            hint(output, ThaumoryCircleEffects.CONTAINMENT, VINCULUM, CHAOS);
            // One for each opposite pair of the third tier.
            hint(output, Thaumory.id("anima_sordes"), ANIMA, SORDES);
            hint(output, Thaumory.id("aurora_vesper"), AURORA, VESPER);
            hint(output, Thaumory.id("abyssus_caelum"), ABYSSUS, CAELUM);
            hint(output, Thaumory.id("sigillum_solutio"), SIGILLUM, SOLUTIO);
            hint(output, Thaumory.id("tartarus_fons"), TARTARUS, FONS);
            hint(output, Thaumory.id("peregrinum_artificium"), PEREGRINUM, ARTIFICIUM);
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
