package one.nxeu.thaumory.fabric.datagen;

import static one.nxeu.thaumory.aspect.ThaumoryAspects.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.data.ItemAspectFile;

/**
 * Hand-written aspects for vanilla items that no recipe or world change reaches (requirements §2.3).
 *
 * <p>Totals follow four tiers: common 8-16, normal 16-32, precious 32-64, very precious 64-128.
 * Items that come in small units (dust, shards, nuggets of loot) sit below the tier of the block
 * they make. Buckets only count what is inside; recipes subtract the empty bucket anyway.
 */
final class VanillaItemAspects {
    private VanillaItemAspects() {}

    static ItemAspectFile build() {
        ItemAspectFileBuilder b = new ItemAspectFileBuilder();
        earth(b);
        oresAndGems(b);
        nether(b);
        end(b);
        waterAndIce(b);
        deepDark(b);
        plants(b);
        crops(b);
        mobDrops(b);
        bossesAndTreasure(b);
        loot(b);
        buckets(b);
        unobtainable(b);
        return b.build();
    }

    private static void earth(ItemAspectFileBuilder b) {
        b.item(Items.DIRT, a(TERRA, 8))
                .item(Items.GRASS_BLOCK, a(TERRA, 8), a(HERBA, 2))
                .item(Items.PODZOL, a(TERRA, 8), a(HERBA, 3))
                .item(Items.MYCELIUM, a(TERRA, 8), a(HERBA, 2), a(UMBRA, 2))
                .item(Items.ROOTED_DIRT, a(TERRA, 8), a(HERBA, 4))
                .item(Items.CLAY_BALL, a(TERRA, 3), a(AQUA, 2))
                .item(Items.SAND, a(TERRA, 6), a(CHAOS, 2))
                .item(Items.RED_SAND, a(TERRA, 6), a(CHAOS, 2), a(IGNIS, 1))
                .item(Items.GRAVEL, a(TERRA, 8), a(CHAOS, 2))
                .item(Items.FLINT, a(TERRA, 4), a(BELLUM, 4), a(IGNIS, 2))
                .item(Items.COBBLESTONE, a(TERRA, 8), a(CHAOS, 1))
                .item(Items.COBBLED_DEEPSLATE, a(TERRA, 8), a(UMBRA, 3))
                .item(Items.TUFF, a(TERRA, 8), a(IGNIS, 2))
                .item(Items.CALCITE, a(TERRA, 6), a(ORDO, 4))
                .item(Items.POINTED_DRIPSTONE, a(TERRA, 4), a(AQUA, 2))
                .item(Items.OBSIDIAN, a(TERRA, 16), a(IGNIS, 8), a(ORDO, 8))
                .item(Items.CRYING_OBSIDIAN, a(TERRA, 16), a(IGNIS, 8), a(ARCANUM, 8), a(AQUA, 4));
    }

    private static void oresAndGems(ItemAspectFileBuilder b) {
        // What players actually take home.
        b.item(Items.COAL, a(IGNIS, 12), a(MORS, 4))
                .item(Items.RAW_IRON, a(METALLUM, 16), a(TERRA, 4))
                .item(Items.RAW_GOLD, a(METALLUM, 16), a(LUX, 8))
                .item(Items.RAW_COPPER, a(METALLUM, 12), a(TEMPESTAS, 4))
                .item(Items.DIAMOND, a(ORDO, 24), a(TERRA, 8), a(LUX, 8))
                .item(Items.EMERALD, a(ORDO, 16), a(VITA, 12), a(LUX, 8))
                .item(Items.LAPIS_LAZULI, a(ARCANUM, 6), a(AQUA, 4))
                .item(Items.REDSTONE, a(TEMPESTAS, 6), a(ARCANUM, 4))
                .item(Items.QUARTZ, a(ORDO, 8), a(LUX, 4), a(IGNIS, 2))
                .item(Items.AMETHYST_SHARD, a(ARCANUM, 8), a(ORDO, 4), a(LUX, 4))
                .item(Items.ANCIENT_DEBRIS, a(METALLUM, 32), a(IGNIS, 16), a(BELLUM, 12));

        // Silk-touched ores: the drop plus the stone around it.
        ore(b, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE, a(IGNIS, 12), a(MORS, 4));
        ore(b, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, a(METALLUM, 16), a(TERRA, 4));
        ore(b, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, a(METALLUM, 16), a(LUX, 8));
        ore(b, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, a(METALLUM, 12), a(TEMPESTAS, 4));
        ore(b, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE, a(ORDO, 24), a(TERRA, 8), a(LUX, 8));
        ore(b, Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE, a(ORDO, 16), a(VITA, 12), a(LUX, 8));
        ore(b, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE, a(ARCANUM, 24), a(AQUA, 16));
        ore(b, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE, a(TEMPESTAS, 24), a(ARCANUM, 16));

        b.item(Items.LARGE_AMETHYST_BUD, a(ARCANUM, 6), a(ORDO, 3), a(LUX, 3))
                .item(Items.MEDIUM_AMETHYST_BUD, a(ARCANUM, 4), a(ORDO, 2), a(LUX, 2))
                .item(Items.SMALL_AMETHYST_BUD, a(ARCANUM, 2), a(ORDO, 1), a(LUX, 1))
                .item(Items.AMETHYST_CLUSTER, a(ARCANUM, 8), a(ORDO, 4), a(LUX, 4))
                .item(Items.CINNABAR, a(TERRA, 8), a(VENENUM, 4), a(IGNIS, 2))
                .item(Items.SULFUR_SPIKE, a(IGNIS, 4), a(VENENUM, 4));
    }

    private static void ore(ItemAspectFileBuilder b, Item ore, Item deepslateOre, AspectStack... drop) {
        b.item(ore, with(drop, a(TERRA, 8)));
        b.item(deepslateOre, with(drop, a(TERRA, 8), a(UMBRA, 3)));
    }

    private static void nether(ItemAspectFileBuilder b) {
        b.item(Items.NETHERRACK, a(TERRA, 4), a(IGNIS, 4))
                .item(Items.CRIMSON_NYLIUM, a(TERRA, 4), a(IGNIS, 6), a(HERBA, 2))
                .item(Items.WARPED_NYLIUM, a(TERRA, 4), a(IGNIS, 4), a(HERBA, 2), a(ARCANUM, 2))
                .item(Items.SOUL_SAND, a(TERRA, 4), a(MORS, 6), a(UMBRA, 2))
                .item(Items.SOUL_SOIL, a(TERRA, 4), a(MORS, 6))
                .item(Items.BASALT, a(TERRA, 8), a(IGNIS, 4))
                .item(Items.BLACKSTONE, a(TERRA, 8), a(IGNIS, 2), a(UMBRA, 2))
                .item(Items.GILDED_BLACKSTONE, a(TERRA, 8), a(IGNIS, 2), a(UMBRA, 2), a(METALLUM, 6), a(LUX, 3))
                .item(Items.NETHER_GOLD_ORE, a(METALLUM, 16), a(LUX, 8), a(TERRA, 4), a(IGNIS, 4))
                .item(Items.NETHER_QUARTZ_ORE, a(TERRA, 4), a(IGNIS, 6), a(ORDO, 8), a(LUX, 4))
                .item(Items.GLOWSTONE_DUST, a(LUX, 4), a(IGNIS, 2))
                .item(Items.SHROOMLIGHT, a(LUX, 8), a(HERBA, 4), a(IGNIS, 4))
                .item(Items.CRIMSON_FUNGUS, a(HERBA, 4), a(IGNIS, 2))
                .item(Items.WARPED_FUNGUS, a(HERBA, 4), a(ARCANUM, 2))
                .item(Items.CRIMSON_ROOTS, a(HERBA, 3), a(IGNIS, 1))
                .item(Items.WARPED_ROOTS, a(HERBA, 3), a(ARCANUM, 1))
                .item(Items.NETHER_SPROUTS, a(HERBA, 2), a(ARCANUM, 1))
                .item(Items.WEEPING_VINES, a(HERBA, 4), a(IGNIS, 2), a(VINCULUM, 2))
                .item(Items.TWISTING_VINES, a(HERBA, 4), a(ARCANUM, 2), a(VINCULUM, 2))
                .item(Items.WARPED_WART_BLOCK, a(HERBA, 8), a(ARCANUM, 4))
                .item(Items.NETHER_WART, a(HERBA, 4), a(ARCANUM, 4), a(MORS, 2));
    }

    private static void end(ItemAspectFileBuilder b) {
        b.item(Items.END_STONE, a(TERRA, 8), a(CHAOS, 4))
                .item(Items.CHORUS_FRUIT, a(HERBA, 4), a(CHAOS, 4), a(ARCANUM, 2))
                .item(Items.CHORUS_FLOWER, a(HERBA, 6), a(CHAOS, 6), a(ARCANUM, 4))
                .item(Items.CHORUS_PLANT, a(HERBA, 4), a(CHAOS, 4))
                .item(Items.SHULKER_SHELL, a(VINCULUM, 16), a(CHAOS, 8), a(ARCANUM, 8))
                .item(Items.DRAGON_BREATH, a(ARCANUM, 16), a(IGNIS, 8), a(CHAOS, 8));
    }

    private static void waterAndIce(ItemAspectFileBuilder b) {
        b.item(Items.ICE, a(AQUA, 8), a(ORDO, 4))
                .item(Items.SNOWBALL, a(AQUA, 2), a(AER, 1))
                .item(Items.PRISMARINE_SHARD, a(AQUA, 4), a(ORDO, 2))
                .item(Items.PRISMARINE_CRYSTALS, a(AQUA, 3), a(LUX, 3))
                .item(Items.SEAGRASS, a(HERBA, 3), a(AQUA, 3))
                .item(Items.KELP, a(HERBA, 4), a(AQUA, 4))
                .item(Items.SEA_PICKLE, a(AQUA, 4), a(LUX, 4), a(BESTIA, 2))
                .item(Items.LILY_PAD, a(HERBA, 4), a(AQUA, 4))
                .item(Items.WET_SPONGE, a(AQUA, 12), a(BESTIA, 8), a(ORDO, 4));
        for (String coral : new String[] {"tube", "brain", "bubble", "fire", "horn"}) {
            b.item(item(coral + "_coral"), a(AQUA, 4), a(VITA, 4), a(BESTIA, 2))
                    .item(item(coral + "_coral_fan"), a(AQUA, 4), a(VITA, 4), a(BESTIA, 2))
                    .item(item(coral + "_coral_block"), a(AQUA, 8), a(VITA, 6), a(BESTIA, 4), a(TERRA, 2));
        }
    }

    private static void deepDark(ItemAspectFileBuilder b) {
        b.item(Items.SCULK, a(UMBRA, 4), a(MORS, 2), a(ARCANUM, 2))
                .item(Items.SCULK_VEIN, a(UMBRA, 1), a(MORS, 1))
                .item(Items.SCULK_CATALYST, a(UMBRA, 16), a(MORS, 16), a(ARCANUM, 8))
                .item(Items.SCULK_SENSOR, a(UMBRA, 8), a(TEMPESTAS, 8), a(ARCANUM, 4))
                .item(Items.SCULK_SHRIEKER, a(UMBRA, 12), a(TEMPESTAS, 8), a(MORS, 8))
                .item(Items.ECHO_SHARD, a(UMBRA, 24), a(ARCANUM, 16), a(TEMPESTAS, 8));
    }

    private static void plants(ItemAspectFileBuilder b) {
        for (String flower : new String[] {"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip",
                "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley"}) {
            b.item(item(flower), a(HERBA, 5), a(VITA, 2), a(LUX, 1));
        }
        b.tag(ItemTags.LOGS, a(HERBA, 16), a(TERRA, 4))
                .tag(ItemTags.SAPLINGS, a(HERBA, 8), a(VITA, 4))
                .tag(ItemTags.LEAVES, a(HERBA, 6), a(AER, 2))
                .item(Items.SUNFLOWER, a(HERBA, 8), a(VITA, 4), a(LUX, 4))
                .item(Items.LILAC, a(HERBA, 8), a(VITA, 4), a(LUX, 2))
                .item(Items.ROSE_BUSH, a(HERBA, 8), a(VITA, 4), a(BELLUM, 2))
                .item(Items.PEONY, a(HERBA, 8), a(VITA, 4), a(LUX, 2))
                .item(Items.PITCHER_PLANT, a(HERBA, 8), a(VITA, 4), a(AQUA, 4))
                .item(Items.WITHER_ROSE, a(HERBA, 4), a(MORS, 8), a(VENENUM, 4))
                .item(Items.TORCHFLOWER, a(HERBA, 6), a(LUX, 4), a(IGNIS, 2))
                .item(Items.OPEN_EYEBLOSSOM, a(HERBA, 4), a(UMBRA, 2), a(LUX, 2))
                .item(Items.CLOSED_EYEBLOSSOM, a(HERBA, 4), a(UMBRA, 4))
                .item(Items.PINK_PETALS, a(HERBA, 3), a(VITA, 2))
                .item(Items.WILDFLOWERS, a(HERBA, 3), a(VITA, 2))
                .item(Items.SHORT_GRASS, a(HERBA, 3), a(AER, 1))
                .item(Items.TALL_GRASS, a(HERBA, 6), a(AER, 2))
                .item(Items.FERN, a(HERBA, 3), a(UMBRA, 1))
                .item(Items.LARGE_FERN, a(HERBA, 6), a(UMBRA, 2))
                .item(Items.DEAD_BUSH, a(HERBA, 1), a(MORS, 3), a(IGNIS, 1))
                .item(item("short_dry_grass"), a(HERBA, 2), a(IGNIS, 1))
                .item(item("tall_dry_grass"), a(HERBA, 4), a(IGNIS, 2))
                .item(Items.BUSH, a(HERBA, 4))
                .item(Items.FIREFLY_BUSH, a(HERBA, 6), a(LUX, 4))
                .item(Items.VINE, a(HERBA, 4), a(VINCULUM, 2))
                .item(Items.GLOW_LICHEN, a(HERBA, 2), a(LUX, 4))
                .item(Items.HANGING_ROOTS, a(HERBA, 3), a(TERRA, 2))
                .item(Items.MOSS_BLOCK, a(HERBA, 6), a(TERRA, 2))
                .item(Items.PALE_MOSS_BLOCK, a(HERBA, 6), a(UMBRA, 2))
                .item(Items.PALE_HANGING_MOSS, a(HERBA, 3), a(UMBRA, 1))
                .item(Items.BIG_DRIPLEAF, a(HERBA, 6), a(AQUA, 2))
                .item(Items.SMALL_DRIPLEAF, a(HERBA, 4), a(AQUA, 2))
                .item(Items.SPORE_BLOSSOM, a(HERBA, 6), a(AER, 4), a(VITA, 2))
                .item(Items.AZALEA, a(HERBA, 8), a(VITA, 2))
                .item(Items.FLOWERING_AZALEA, a(HERBA, 8), a(VITA, 4))
                .item(Items.CACTUS, a(HERBA, 6), a(BELLUM, 2), a(AQUA, 2))
                .item(Items.CACTUS_FLOWER, a(HERBA, 4), a(LUX, 2))
                .item(Items.SUGAR_CANE, a(HERBA, 6), a(AQUA, 2))
                .item(Items.BAMBOO, a(HERBA, 4), a(AER, 2))
                .item(Items.STRIPPED_BAMBOO_BLOCK, a(HERBA, 24), a(AER, 12))
                .item(Items.COBWEB, a(VINCULUM, 8), a(BESTIA, 2))
                .item(Items.MANGROVE_ROOTS, a(HERBA, 6), a(AQUA, 2), a(TERRA, 2))
                .item(Items.BROWN_MUSHROOM, a(HERBA, 4), a(UMBRA, 4))
                .item(Items.RED_MUSHROOM, a(HERBA, 4), a(UMBRA, 4))
                .item(Items.BROWN_MUSHROOM_BLOCK, a(HERBA, 8), a(UMBRA, 6))
                .item(Items.RED_MUSHROOM_BLOCK, a(HERBA, 8), a(UMBRA, 6))
                .item(Items.MUSHROOM_STEM, a(HERBA, 8), a(UMBRA, 4))
                .item(Items.SHELF_MUSHROOM, a(HERBA, 6), a(UMBRA, 4))
                .item(item("red_shrub"), a(HERBA, 4), a(IGNIS, 1));
    }

    private static void crops(ItemAspectFileBuilder b) {
        b.item(Items.WHEAT_SEEDS, a(HERBA, 2), a(VITA, 2))
                .item(Items.WHEAT, a(HERBA, 6), a(VITA, 2))
                .item(Items.CARROT, a(HERBA, 4), a(VITA, 3), a(LUX, 1))
                .item(Items.POTATO, a(HERBA, 4), a(VITA, 3), a(TERRA, 1))
                .item(Items.POISONOUS_POTATO, a(HERBA, 4), a(VENENUM, 4))
                .item(Items.BEETROOT, a(HERBA, 4), a(VITA, 3))
                .item(Items.BEETROOT_SEEDS, a(HERBA, 2), a(VITA, 1))
                .item(Items.MELON_SLICE, a(HERBA, 2), a(AQUA, 2), a(VITA, 1))
                .item(Items.PUMPKIN, a(HERBA, 8), a(VITA, 4))
                .item(Items.APPLE, a(HERBA, 4), a(VITA, 4))
                .item(Items.SWEET_BERRIES, a(HERBA, 3), a(VITA, 2), a(BELLUM, 1))
                .item(Items.GLOW_BERRIES, a(HERBA, 3), a(VITA, 2), a(LUX, 2))
                .item(Items.COCOA_BEANS, a(HERBA, 4), a(VITA, 2))
                .item(Items.TORCHFLOWER_SEEDS, a(HERBA, 3), a(LUX, 2))
                .item(Items.PITCHER_POD, a(HERBA, 3), a(AQUA, 2));
    }

    private static void mobDrops(ItemAspectFileBuilder b) {
        b.item(Items.BEEF, a(BESTIA, 8), a(VITA, 4))
                .item(Items.PORKCHOP, a(BESTIA, 8), a(VITA, 4))
                .item(Items.MUTTON, a(BESTIA, 6), a(VITA, 4))
                .item(Items.CHICKEN, a(BESTIA, 4), a(VITA, 3), a(AER, 1))
                .item(Items.RABBIT, a(BESTIA, 4), a(VITA, 3))
                .item(Items.COD, a(BESTIA, 4), a(AQUA, 4))
                .item(Items.SALMON, a(BESTIA, 4), a(AQUA, 4), a(VITA, 2))
                .item(Items.TROPICAL_FISH, a(BESTIA, 3), a(AQUA, 3), a(LUX, 2))
                .item(Items.PUFFERFISH, a(BESTIA, 3), a(AQUA, 3), a(VENENUM, 6))
                .item(Items.ROTTEN_FLESH, a(BESTIA, 4), a(MORS, 6))
                .item(Items.BONE, a(MORS, 8), a(ORDO, 2))
                .item(Items.STRING, a(BESTIA, 2), a(VINCULUM, 4))
                .item(Items.SPIDER_EYE, a(BESTIA, 2), a(VENENUM, 6))
                .item(Items.FEATHER, a(AER, 6), a(BESTIA, 2))
                .item(Items.RABBIT_HIDE, a(BESTIA, 4), a(VINCULUM, 2))
                .item(Items.RABBIT_FOOT, a(BESTIA, 4), a(VITA, 4), a(ARCANUM, 4))
                .item(Items.GUNPOWDER, a(IGNIS, 8), a(BELLUM, 6), a(CHAOS, 2))
                .item(Items.ENDER_PEARL, a(ARCANUM, 16), a(CHAOS, 12), a(AER, 8))
                .item(Items.BLAZE_ROD, a(IGNIS, 24), a(ARCANUM, 8))
                .item(Items.BREEZE_ROD, a(AER, 24), a(TEMPESTAS, 12), a(ARCANUM, 4))
                .item(Items.GHAST_TEAR, a(AQUA, 12), a(VITA, 12), a(MORS, 8))
                .item(Items.PHANTOM_MEMBRANE, a(AER, 12), a(MORS, 8), a(UMBRA, 4))
                .item(Items.INK_SAC, a(AQUA, 4), a(UMBRA, 6))
                .item(Items.GLOW_INK_SAC, a(AQUA, 4), a(LUX, 8))
                .item(Items.TURTLE_SCUTE, a(BESTIA, 8), a(AQUA, 8), a(ORDO, 4))
                .item(Items.ARMADILLO_SCUTE, a(BESTIA, 8), a(TERRA, 4), a(ORDO, 4))
                .item(Items.NAUTILUS_SHELL, a(AQUA, 12), a(ORDO, 8), a(BESTIA, 4))
                .item(Items.HONEYCOMB, a(HERBA, 4), a(BESTIA, 2), a(ORDO, 2))
                .item(Items.HONEY_BOTTLE, a(HERBA, 4), a(VITA, 4), a(BESTIA, 2))
                .item(Items.BEE_NEST, a(HERBA, 12), a(BESTIA, 8), a(ORDO, 4))
                .item(Items.SLIME_BALL, a(AQUA, 4), a(VITA, 4), a(VINCULUM, 4))
                .item(Items.RESIN_CLUMP, a(HERBA, 4), a(VINCULUM, 4), a(UMBRA, 2))
                .item(Items.OCHRE_FROGLIGHT, a(LUX, 12), a(BESTIA, 4), a(IGNIS, 2))
                .item(Items.VERDANT_FROGLIGHT, a(LUX, 12), a(BESTIA, 4), a(HERBA, 2))
                .item(Items.PEARLESCENT_FROGLIGHT, a(LUX, 12), a(BESTIA, 4), a(ARCANUM, 2))
                .item(Items.EGG, a(BESTIA, 4), a(VITA, 4))
                .item(Items.BROWN_EGG, a(BESTIA, 4), a(VITA, 4))
                .item(Items.BLUE_EGG, a(BESTIA, 4), a(VITA, 4))
                .item(Items.TURTLE_EGG, a(BESTIA, 8), a(VITA, 8), a(AQUA, 4))
                .item(Items.SNIFFER_EGG, a(BESTIA, 16), a(VITA, 16), a(TERRA, 8))
                .item(Items.GOAT_HORN, a(BESTIA, 8), a(AER, 8), a(TEMPESTAS, 4))
                .item(Items.SKELETON_SKULL, a(MORS, 24), a(BESTIA, 4))
                .item(Items.ZOMBIE_HEAD, a(MORS, 16), a(BESTIA, 8))
                .item(Items.CREEPER_HEAD, a(BELLUM, 16), a(IGNIS, 8), a(HERBA, 4))
                .item(Items.PIGLIN_HEAD, a(BESTIA, 12), a(MORS, 8), a(METALLUM, 4))
                .item(Items.WITHER_SKELETON_SKULL, a(MORS, 32), a(UMBRA, 16), a(BELLUM, 8));
    }

    private static void bossesAndTreasure(ItemAspectFileBuilder b) {
        b.item(Items.NETHER_STAR, a(ARCANUM, 48), a(LUX, 32), a(MORS, 32), a(ORDO, 16))
                .item(Items.DRAGON_EGG, a(ARCANUM, 64), a(VITA, 32), a(BESTIA, 32))
                .item(Items.DRAGON_HEAD, a(BESTIA, 32), a(ARCANUM, 32), a(IGNIS, 16))
                .item(Items.ELYTRA, a(AER, 64), a(ARCANUM, 32), a(BESTIA, 16))
                .item(Items.HEAVY_CORE, a(METALLUM, 48), a(TEMPESTAS, 16), a(VINCULUM, 16))
                .item(Items.HEART_OF_THE_SEA, a(AQUA, 32), a(ARCANUM, 16), a(LUX, 8))
                .item(Items.TOTEM_OF_UNDYING, a(VITA, 48), a(ARCANUM, 24), a(ORDO, 16))
                .item(Items.ENCHANTED_GOLDEN_APPLE, a(VITA, 64), a(ARCANUM, 32), a(LUX, 16), a(METALLUM, 8))
                .item(Items.TRIDENT, a(AQUA, 24), a(BELLUM, 16), a(METALLUM, 16))
                .item(Items.EXPERIENCE_BOTTLE, a(ARCANUM, 12), a(VITA, 4));
    }

    private static void loot(ItemAspectFileBuilder b) {
        b.tag(ItemTags.DECORATED_POT_SHERDS, a(TERRA, 8), a(ORDO, 4), a(ARCANUM, 2))
                .item(Items.DISC_FRAGMENT_5, a(AER, 2), a(ORDO, 2), a(ARCANUM, 1))
                .item(Items.IRON_HORSE_ARMOR, a(METALLUM, 24), a(BESTIA, 4))
                .item(Items.GOLDEN_HORSE_ARMOR, a(METALLUM, 24), a(LUX, 8), a(BESTIA, 4))
                .item(Items.DIAMOND_HORSE_ARMOR, a(ORDO, 32), a(LUX, 8), a(BESTIA, 4))
                .item(Items.COPPER_HORSE_ARMOR, a(METALLUM, 18), a(TEMPESTAS, 6), a(BESTIA, 4))
                .item(Items.IRON_NAUTILUS_ARMOR, a(METALLUM, 24), a(AQUA, 4))
                .item(Items.GOLDEN_NAUTILUS_ARMOR, a(METALLUM, 24), a(LUX, 8), a(AQUA, 4))
                .item(Items.DIAMOND_NAUTILUS_ARMOR, a(ORDO, 32), a(LUX, 8), a(AQUA, 4))
                .item(Items.COPPER_NAUTILUS_ARMOR, a(METALLUM, 18), a(TEMPESTAS, 6), a(AQUA, 4))
                .item(Items.CHAINMAIL_HELMET, a(METALLUM, 15), a(VINCULUM, 5))
                .item(Items.CHAINMAIL_CHESTPLATE, a(METALLUM, 24), a(VINCULUM, 8))
                .item(Items.CHAINMAIL_LEGGINGS, a(METALLUM, 21), a(VINCULUM, 7))
                .item(Items.CHAINMAIL_BOOTS, a(METALLUM, 12), a(VINCULUM, 4))
                .item(Items.BELL, a(METALLUM, 16), a(LUX, 4), a(ORDO, 4))
                .item(Items.TRIAL_KEY, a(ARCANUM, 8), a(METALLUM, 4), a(ORDO, 4))
                .item(Items.OMINOUS_TRIAL_KEY, a(ARCANUM, 8), a(CHAOS, 8), a(METALLUM, 4), a(ORDO, 4))
                .item(Items.OMINOUS_BOTTLE, a(ARCANUM, 8), a(CHAOS, 8), a(BELLUM, 4))
                .item(item("copper_golem_statue"), a(METALLUM, 16), a(ARCANUM, 8), a(ORDO, 4));
        for (Item item : BuiltInRegistries.ITEM) {
            String path = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (path.equals("netherite_upgrade_smithing_template")) {
                b.item(item, a(METALLUM, 16), a(IGNIS, 16), a(ARCANUM, 12), a(ORDO, 8));
            } else if (path.endsWith("_armor_trim_smithing_template")) {
                b.item(item, a(ORDO, 16), a(ARCANUM, 12), a(TERRA, 4));
            } else if (path.startsWith("music_disc_")) {
                b.item(item, a(AER, 8), a(ORDO, 8), a(ARCANUM, 4));
            } else if (path.endsWith("_banner_pattern") && isLootOnlyPattern(path)) {
                b.item(item, a(ORDO, 8), a(ARCANUM, 4), a(HERBA, 2));
            }
        }
    }

    /** Patterns without a crafting recipe, found only in structures. */
    private static boolean isLootOnlyPattern(String path) {
        return path.equals("flow_banner_pattern") || path.equals("guster_banner_pattern")
                || path.equals("globe_banner_pattern") || path.equals("piglin_banner_pattern");
    }

    private static void buckets(ItemAspectFileBuilder b) {
        b.item(Items.WATER_BUCKET, a(AQUA, 8))
                .item(Items.LAVA_BUCKET, a(IGNIS, 16), a(TERRA, 4))
                .item(Items.MILK_BUCKET, a(VITA, 6), a(BESTIA, 4), a(AQUA, 2))
                .item(Items.POWDER_SNOW_BUCKET, a(AQUA, 6), a(AER, 4), a(ORDO, 2))
                .item(Items.COD_BUCKET, a(BESTIA, 4), a(AQUA, 12))
                .item(Items.SALMON_BUCKET, a(BESTIA, 4), a(AQUA, 12), a(VITA, 2))
                .item(Items.TROPICAL_FISH_BUCKET, a(BESTIA, 3), a(AQUA, 11), a(LUX, 2))
                .item(Items.PUFFERFISH_BUCKET, a(BESTIA, 3), a(AQUA, 11), a(VENENUM, 6))
                .item(Items.AXOLOTL_BUCKET, a(BESTIA, 12), a(AQUA, 12), a(VITA, 8))
                .item(Items.TADPOLE_BUCKET, a(BESTIA, 4), a(AQUA, 8), a(VITA, 4))
                .item(Items.SULFUR_CUBE_BUCKET, a(BESTIA, 8), a(VENENUM, 8), a(AQUA, 4));
    }

    /** Creative-only, unobtainable, or items whose worth lives in components (decided later). */
    private static void unobtainable(ItemAspectFileBuilder b) {
        b.none(Items.AIR, Items.BEDROCK, Items.BARRIER, Items.LIGHT, Items.STRUCTURE_VOID, Items.STRUCTURE_BLOCK,
                Items.JIGSAW, Items.COMMAND_BLOCK, Items.CHAIN_COMMAND_BLOCK, Items.REPEATING_COMMAND_BLOCK,
                Items.COMMAND_BLOCK_MINECART, Items.DEBUG_STICK, Items.KNOWLEDGE_BOOK, Items.TEST_BLOCK,
                Items.TEST_INSTANCE_BLOCK, Items.SPAWNER, Items.TRIAL_SPAWNER, Items.VAULT, Items.END_PORTAL_FRAME,
                Items.REINFORCED_DEEPSLATE, Items.BUDDING_AMETHYST, Items.FARMLAND, Items.DIRT_PATH,
                Items.PETRIFIED_OAK_SLAB, Items.FROGSPAWN, Items.SUSPICIOUS_SAND, Items.SUSPICIOUS_GRAVEL,
                Items.PLAYER_HEAD);
        b.none(Items.INFESTED_STONE, Items.INFESTED_COBBLESTONE, Items.INFESTED_STONE_BRICKS,
                Items.INFESTED_MOSSY_STONE_BRICKS, Items.INFESTED_CRACKED_STONE_BRICKS,
                Items.INFESTED_CHISELED_STONE_BRICKS, Items.INFESTED_DEEPSLATE);
        // Potions, maps and books vary by component; their aspects come with potion brewing (M3).
        b.none(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW, Items.ENCHANTED_BOOK,
                Items.WRITTEN_BOOK, Items.FILLED_MAP, Items.FIREWORK_STAR);
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SpawnEggItem) {
                b.none(item);
            } else if (BuiltInRegistries.ITEM.getKey(item).getPath().endsWith("_map") && item != Items.MAP) {
                b.none(item); // explorer maps
            }
        }
    }

    private static Item item(String path) {
        Identifier id = Identifier.withDefaultNamespace(path);
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("No such item: " + id);
        }
        return BuiltInRegistries.ITEM.getValue(id);
    }

    private static AspectStack a(Aspect aspect, int amount) {
        return new AspectStack(aspect, amount);
    }

    private static AspectStack[] with(AspectStack[] base, AspectStack... extra) {
        AspectStack[] all = new AspectStack[base.length + extra.length];
        System.arraycopy(base, 0, all, 0, base.length);
        System.arraycopy(extra, 0, all, base.length, extra.length);
        return all;
    }
}
