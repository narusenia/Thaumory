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
import java.util.stream.Stream;
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
    static final Identifier WAND_FOCI = Thaumory.id("wand_foci");
    static final Identifier BATTLE_FOCI = Thaumory.id("battle_foci");
    static final Identifier WORK_FOCI = Thaumory.id("work_foci");
    static final Identifier TRANSCRIPTS = Thaumory.id("transcripts");
    static final Identifier CRYSTALS = Thaumory.id("crystals");
    static final Identifier RUINS = Thaumory.id("ruins");
    static final Identifier JARS = Thaumory.id("jars");
    static final Identifier PIPES = Thaumory.id("pipes");
    static final Identifier PIPE_CONTROL = Thaumory.id("pipe_control");
    static final Identifier POUCH = Thaumory.id("pouch");
    static final Identifier POLLUTION = Thaumory.id("pollution");
    static final Identifier CONTAINMENT = Thaumory.id("containment");
    static final Identifier WALL_CIRCLES = Thaumory.id("wall_circles");
    static final Identifier CIRCLE_RANKS = Thaumory.id("circle_ranks");
    static final Identifier SUB_CIRCLES = Thaumory.id("sub_circles");
    static final Identifier FARM_CIRCLES = Thaumory.id("farm_circles");
    static final Identifier INDUSTRY_CIRCLES = Thaumory.id("industry_circles");
    static final Identifier DEFENCE_CIRCLES = Thaumory.id("defence_circles");
    static final Identifier LIFE_CIRCLES = Thaumory.id("life_circles");
    static final Identifier INFUSION = Thaumory.id("infusion");
    static final Identifier SCROLLS = Thaumory.id("scrolls");
    static final Identifier CIRCLE_STONES = Thaumory.id("circle_stones");
    static final Identifier AMULETS = Thaumory.id("amulets");
    static final Identifier CHARGING = Thaumory.id("charging");
    static final Identifier WAND_PARTS = Thaumory.id("wand_parts");

    static final Identifier BASICS = Chapter.BASICS;
    static final Identifier ALCHEMY = Thaumory.id("alchemy");
    static final Identifier CIRCLES_CATEGORY = Thaumory.id("circles");
    static final Identifier METALS = Thaumory.id("metals");
    static final Identifier ARTIFICE = Thaumory.id("artifice");
    static final Identifier INFUSION_CATEGORY = Thaumory.id("infusion");

    private ResearchProvider() {}

    /** Writes {@code data/thaumory/thaumory/research/chapter/*.json}. */
    static final class Chapters extends FabricCodecDataProvider<Chapter> {
        Chapters(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries, PackOutput.Target.DATA_PACK, "thaumory/research/chapter", Chapter.CODEC);
        }

        @Override
        protected void configure(BiConsumer<Identifier, Chapter> output, HolderLookup.Provider registries) {
            // Basics
            output.accept(BEGINNING, chapter(ThaumoryItems.ARCANE_CODEX.get(), BASICS, 0, 0, List.of(), List.of(), List.of()));
            output.accept(ASPECTS, chapter(ThaumoryItems.ARCANE_LOUPE.get(), BASICS, 1, 0, List.of(BEGINNING),
                    List.of(ResearchCondition.Aspects.count(1)), List.of()));
            output.accept(INQUIRY, chapter(ThaumoryItems.WAND.get(), BASICS, 2, 0, List.of(ASPECTS),
                    List.of(ResearchCondition.Aspects.count(12)), List.of()));
            output.accept(TRANSCRIPTS, chapter(ThaumoryItems.TRANSCRIPT.get(), BASICS, 3, 0, List.of(INQUIRY),
                    List.of(ResearchCondition.Aspects.count(12)), List.of()));
            output.accept(CRYSTALS, chapter(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get(), BASICS, 0, 1, List.of(BEGINNING),
                    List.of(scanned(ThaumoryItems.ARCANE_CRYSTAL_SHARD.get())), List.of()));
            output.accept(RUINS, chapter(Items.MOSSY_STONE_BRICKS, BASICS, 1, 1, List.of(ASPECTS),
                    List.of(scanned(Items.MOSSY_STONE_BRICKS)), List.of()));
            output.accept(MONOCLE, chapter(ThaumoryItems.MONOCLE.get(), BASICS, 2, 1, List.of(ASPECTS, CRUCIBLE),
                    List.of(ResearchCondition.Aspects.all(List.of(LUX.id(), ARCANUM.id()))), List.of(alchemy("monocle"))));
            // Alchemy
            output.accept(CRUCIBLE, chapter(ThaumoryItems.CRUCIBLE.get(), ALCHEMY, 0, 0, List.of(BEGINNING),
                    List.of(scanned(ThaumoryItems.CRUCIBLE.get())), List.of()));
            output.accept(RUNES, chapter(ThaumoryItems.BLANK_RUNE.get(), ALCHEMY, 1, 0, List.of(ASPECTS, CRUCIBLE),
                    List.of(ResearchCondition.Aspects.count(3)), List.of(alchemy("blank_rune"), alchemy("chalk"), craft("circle_core"))));
            // Artifice
            output.accept(JARS, chapter(ThaumoryItems.JAR.get(), ARTIFICE, 0, 0, List.of(CRUCIBLE),
                    List.of(scanned(Items.GLASS)), List.of(craft("jar"), craft("label"))));
            output.accept(PIPES, chapter(ThaumoryItems.PIPE.get(), ARTIFICE, 1, 0, List.of(JARS),
                    List.of(scanned(ThaumoryItems.JAR.get())), List.of(craft("pipe"))));
            output.accept(PIPE_CONTROL, chapter(ThaumoryItems.VALVE.get(), ARTIFICE, 2, 0, List.of(PIPES),
                    List.of(scanned(ThaumoryItems.PIPE.get())), List.of(craft("filter_pipe"), craft("valve"), craft("pump"))));
            output.accept(POUCH, chapter(ThaumoryItems.ESSENTIA_POUCH.get(), ARTIFICE, 1, 1, List.of(JARS, ARCANE_METALS),
                    List.of(scanned(Items.LEATHER)), List.of(craft("essentia_pouch"))));
            // Circles
            output.accept(CIRCLES, chapter(ThaumoryItems.CIRCLE_CORE.get(), CIRCLES_CATEGORY, 0, 0, List.of(RUNES),
                    List.of(circles(true, 1)),
                    List.of(alchemy("amplifying_chalk"), alchemy("extending_chalk"), alchemy("economizing_chalk"), alchemy("stabilizing_chalk"))));
            output.accept(FLUX, chapter(ThaumoryItems.POLLUTED_SOIL.get(), CIRCLES_CATEGORY, 1, 0, List.of(CIRCLES),
                    List.of(circles(false, 1)), List.of()));
            output.accept(POLLUTION, chapter(ThaumoryItems.POLLUTED_STONE.get(), CIRCLES_CATEGORY, 2, 0, List.of(FLUX),
                    List.of(scanned(ThaumoryItems.POLLUTED_SOIL.get())), List.of()));
            output.accept(CONTAINMENT, chapter(ThaumoryItems.FLUX_CRYSTAL.get(), CIRCLES_CATEGORY, 3, 0, List.of(POLLUTION),
                    List.of(ResearchCondition.Aspects.all(List.of(SORDES.id()))), List.of()));
            output.accept(WALL_CIRCLES, chapter(ThaumoryItems.CHALK.get(), CIRCLES_CATEGORY, 0, 1, List.of(CIRCLES),
                    List.of(circles(true, 3)), List.of()));
            output.accept(CIRCLE_RANKS, chapter(ThaumoryItems.ARCANE_IRON_CIRCLE_CORE.get(), CIRCLES_CATEGORY, 1, 1, List.of(CIRCLES, ARCANE_METALS),
                    List.of(scanned(ThaumoryItems.ARCANE_IRON.ingot().get())),
                    List.of(craft("arcane_iron_circle_core"), craft("aether_silver_circle_core"))));
            output.accept(SUB_CIRCLES, chapter(ThaumoryItems.AETHER_SILVER_CIRCLE_CORE.get(), CIRCLES_CATEGORY, 2, 1, List.of(CIRCLE_RANKS),
                    List.of(circles(true, 5)), List.of()));
            output.accept(FARM_CIRCLES, chapter(Items.WHEAT, CIRCLES_CATEGORY, 0, 2, List.of(CIRCLES),
                    List.of(ResearchCondition.Aspects.all(List.of(HERBA.id()))), List.of()));
            output.accept(INDUSTRY_CIRCLES, chapter(Items.FURNACE, CIRCLES_CATEGORY, 1, 2, List.of(CIRCLES),
                    List.of(ResearchCondition.Aspects.all(List.of(METALLUM.id()))), List.of()));
            output.accept(DEFENCE_CIRCLES, chapter(Items.SHIELD, CIRCLES_CATEGORY, 2, 2, List.of(CIRCLES),
                    List.of(ResearchCondition.Aspects.all(List.of(BELLUM.id()))), List.of()));
            output.accept(LIFE_CIRCLES, chapter(Items.FEATHER, CIRCLES_CATEGORY, 3, 2, List.of(CIRCLES),
                    List.of(ResearchCondition.Aspects.all(List.of(VITA.id()))), List.of()));
            // Infusion
            output.accept(INFUSION, chapter(ThaumoryItems.PEDESTAL.get(), INFUSION_CATEGORY, 0, 0, List.of(CIRCLES),
                    List.of(circles(true, 2)), List.of(craft("pedestal"))));
            output.accept(SCROLLS, chapter(ThaumoryItems.SCROLL.get(), INFUSION_CATEGORY, 1, 0, List.of(INFUSION),
                    List.of(scanned(Items.PAPER)), List.of(alchemy("blank_scroll"))));
            output.accept(CIRCLE_STONES, chapter(ThaumoryItems.BLANK_CIRCLE_STONE.get(), INFUSION_CATEGORY, 2, 0, List.of(INFUSION),
                    List.of(scanned(Items.STONE_BRICKS)), List.of(craft("blank_circle_stone"))));
            output.accept(AMULETS, chapter(ThaumoryItems.AMULET.get(), INFUSION_CATEGORY, 1, 1, List.of(INFUSION, ARCANE_METALS),
                    List.of(scanned(Items.GOLD_INGOT)), List.of(craft("amulet"))));
            output.accept(CHARGING, chapter(ThaumoryItems.RUNE.get(), INFUSION_CATEGORY, 2, 1, List.of(INFUSION),
                    List.of(circles(true, 3)), List.of()));
            // Metals and equipment
            output.accept(ARCANE_METALS, chapter(ThaumoryItems.ARCANE_IRON.ingot().get(), METALS, 0, 0, List.of(CIRCLES),
                    List.of(scanned(Items.IRON_INGOT)), with(alchemy("arcane_iron_ingot"), equipment("arcane_iron"))));
            output.accept(AETHER_SILVER, chapter(ThaumoryItems.AETHER_SILVER.ingot().get(), METALS, 1, 0, List.of(ARCANE_METALS),
                    List.of(scanned(ThaumoryItems.ARCANE_IRON.ingot().get()), ResearchCondition.Aspects.all(List.of(AER.id(), LUX.id()))),
                    with(alchemy("aether_silver_ingot"), equipment("aether_silver"))));
            output.accept(WAND_PARTS, chapter(ThaumoryItems.GOLD_WAND_CAP.get(), METALS, 0, 1, List.of(ARCANE_METALS),
                    List.of(scanned(Items.STICK)),
                    List.of(craft("arcane_iron_wand_cap"), craft("aether_silver_wand_cap"), alchemy("crystal_wand_core"))));
            output.accept(WAND_FOCI, chapter(ThaumoryItems.LIGHT_FOCUS.get(), METALS, 1, 1, List.of(ARCANE_METALS),
                    List.of(scanned(Items.GLOWSTONE_DUST)), List.of(craft("blank_focus"), alchemy("light_focus"))));
            output.accept(BATTLE_FOCI, chapter(ThaumoryItems.FIRE_FOCUS.get(), METALS, 2, 1, List.of(WAND_FOCI),
                    List.of(ResearchCondition.Aspects.all(List.of(TEMPESTAS.id()))),
                    List.of(alchemy("fire_focus"), alchemy("frost_focus"), alchemy("lightning_focus"))));
            output.accept(WORK_FOCI, chapter(ThaumoryItems.DIGGING_FOCUS.get(), METALS, 1, 2, List.of(WAND_FOCI),
                    List.of(scanned(Items.DIAMOND_PICKAXE)),
                    List.of(alchemy("digging_focus"), alchemy("leap_focus"), alchemy("exchange_focus"))));
        }

        private static ResearchCondition scanned(ItemLike item) {
            return ResearchCondition.Scanned.item(key(item));
        }

        private static ResearchCondition circles(boolean success, int count) {
            return new ResearchCondition.Circles(success, count, Optional.empty());
        }

        /** The crafting-table recipes for a metal's five tools and four pieces of armor. */
        private static List<Identifier> equipment(String metal) {
            return Stream.of("axe", "hoe", "pickaxe", "shovel", "sword", "helmet", "chestplate", "leggings", "boots")
                    .map(piece -> craft(metal + "_" + piece)).toList();
        }

        private static List<Identifier> with(Identifier first, List<Identifier> rest) {
            return Stream.concat(Stream.of(first), rest.stream()).toList();
        }

        private static Identifier craft(String name) {
            return Thaumory.id(name);
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
            output.accept(ARTIFICE, new Category(Chapters.key(ThaumoryItems.PIPE.get()), 2, block("cut_copper")));
            output.accept(CIRCLES_CATEGORY, new Category(Chapters.key(ThaumoryItems.CIRCLE_CORE.get()), 3, block("end_stone")));
            output.accept(INFUSION_CATEGORY, new Category(Chapters.key(ThaumoryItems.PEDESTAL.get()), 4, block("quartz_block_side")));
            output.accept(METALS, new Category(Chapters.key(ThaumoryItems.ARCANE_IRON.ingot().get()), 5, block("iron_block")));
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
