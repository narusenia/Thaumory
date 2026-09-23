package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.entity.ThaumoryEntities;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.item.ThaumoryItems;

/** Every user-facing string, in English and Japanese. Aspect names stay Latin in both. */
abstract sealed class ThaumoryLanguageProvider extends FabricLanguageProvider {
    private ThaumoryLanguageProvider(FabricPackOutput output, String language, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, language, registries);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
        for (Aspect aspect : ThaumoryAspects.ALL) {
            String path = aspect.id().getPath();
            builder.add(aspect.translationKey(), Character.toUpperCase(path.charAt(0)) + path.substring(1));
        }
        translations(builder);
    }

    protected abstract void translations(TranslationBuilder builder);

    static final class English extends ThaumoryLanguageProvider {
        English(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, "en_us", registries);
        }

        @Override
        protected void translations(TranslationBuilder builder) {
            builder.add("tooltip.thaumory.aspects", "Aspects:");
            builder.add(ThaumoryItems.ARCANE_LOUPE.get(), "Arcane Loupe");
            builder.add(ThaumoryItems.WAND.get(), "Wand");
            builder.add("message.thaumory.wand.started", "The circle comes to life");
            builder.add("message.thaumory.wand.stopped", "The circle falls still");
            builder.add("message.thaumory.wand.triggered", "The circle goes off");
            builder.add("message.thaumory.wand.no_rings", "No ring holds around the Core");
            builder.add("message.thaumory.wand.no_response", "The circle does not answer");
            builder.add("message.thaumory.wand.no_essentia", "The Core lacks Essentia");
            builder.add("message.thaumory.wand.no_target", "The circle finds nothing to act on");
            builder.add("message.thaumory.wand.misfired", "The circle misfires and Flux leaks out");
            builder.add("message.thaumory.wand.overloaded", "Overloaded with Flux, the circle bursts!");
            builder.add("message.thaumory.circle.discovered", "You found a new circle: %s");
            builder.add("hud.thaumory.flux.amount", "Flux here: %s");
            builder.add("hud.thaumory.block.unscanned", "Not scanned");
            builder.add("hud.thaumory.block.no_aspects", "No aspects");
            builder.add("hud.thaumory.flux.stage.none", "Calm");
            builder.add("hud.thaumory.flux.stage.stagnation", "Stagnation");
            builder.add("hud.thaumory.flux.stage.erosion", "Erosion");
            builder.add("hud.thaumory.flux.stage.manifestation", "Manifestation");
            builder.add("hud.thaumory.flux.stage.overload", "Overload");
            builder.add("codex.thaumory.flux_warning.stagnation", "The air here is stagnant with Flux");
            builder.add("codex.thaumory.flux_warning.erosion", "Flux is eating away at the land here");
            builder.add("codex.thaumory.flux_warning.manifestation", "Flux is taking shape here");
            builder.add("codex.thaumory.flux_warning.overload", "Flux overflows here; circles may burst");
            builder.add("itemGroup.thaumory", "Thaumory");
            builder.add(ThaumoryItems.ARCANE_CODEX.get(), "Arcane Codex");
            builder.add(ThaumoryItems.BLANK_RUNE.get(), "Blank Rune");
            builder.add(ThaumoryItems.RUNE.get(), "Rune");
            builder.add("tooltip.thaumory.rune.aspect", "Aspect: %s");
            builder.add("message.thaumory.rune.not_enough", "A rune takes %s Essentia of one aspect");
            builder.add(ThaumoryBlocks.JAR.get(), "Jar");
            builder.add(ThaumoryItems.LABEL.get(), "Label");
            builder.add("message.thaumory.jar.cannot_label", "A label only goes on a jar holding exactly one aspect");
            builder.add("tooltip.thaumory.jar.label", "Label: %s");
            builder.add("tooltip.thaumory.jar.essentia", "Essentia %s");
            builder.add(ThaumoryItems.CHALK.get(), "Chalk");
            builder.add(ThaumoryItems.AMPLIFYING_CHALK.get(), "Amplifying Chalk");
            builder.add(ThaumoryItems.EXTENDING_CHALK.get(), "Extending Chalk");
            builder.add(ThaumoryItems.ECONOMIZING_CHALK.get(), "Economizing Chalk");
            builder.add(ThaumoryItems.STABILIZING_CHALK.get(), "Stabilizing Chalk");
            builder.add(ThaumoryBlocks.CORE.get(), "Circle Core");
            builder.add(ThaumoryBlocks.POLLUTED_SOIL.get(), "Polluted Soil");
            builder.add(ThaumoryBlocks.POLLUTED_STONE.get(), "Polluted Stone");
            builder.add(ThaumoryEntities.VOID_REMNANT.get(), "Void Remnant");
            builder.add("message.thaumory.core.full", "All three rune slots are full");
            builder.add("hud.thaumory.core.runes", "Runes:");
            builder.add("hud.thaumory.core.empty_slot", "—");
            builder.add("hud.thaumory.core.rings", "Rings %s/%s");
            builder.add("hud.thaumory.core.node", "Ring %s, %s: %s");
            builder.add("hud.thaumory.core.side.north", "north");
            builder.add("hud.thaumory.core.side.east", "east");
            builder.add("hud.thaumory.core.side.south", "south");
            builder.add("hud.thaumory.core.side.west", "west");
            builder.add("hud.thaumory.core.ignored", "%s modifiers off the nodes (no effect)");
            builder.add("hud.thaumory.core.instability", "Instability %s/%s");
            builder.add("hud.thaumory.core.unstable", "Unstable: activations may release Flux");
            builder.add("hud.thaumory.core.running", "Running");
            builder.add("hud.thaumory.core.stopped", "Stopped");
            builder.add("hud.thaumory.core.upkeep.triggered", "Each activation: %s of each effect rune, 1 of slot 3");
            builder.add("hud.thaumory.core.upkeep.sustained", "1 of each effect rune every %s s");
            builder.add("hud.thaumory.core.effect", "Circle of %s");
            builder.add("hud.thaumory.core.unknown_circle", "Unknown circle");
            builder.add("hud.thaumory.core.failed_circle", "A combination that failed");
            builder.add(ThaumoryCircleEffects.LIGHT.toLanguageKey("circle_effect"), "Light");
            builder.add(ThaumoryCircleEffects.TELEPORT.toLanguageKey("circle_effect"), "Teleportation");
            builder.add(ThaumoryCircleEffects.PURIFICATION.toLanguageKey("circle_effect"), "Purification");
            builder.add(ThaumoryCircleEffects.WARD.toLanguageKey("circle_effect"), "Warding");
            builder.add(ThaumoryCircleEffects.GROWTH.toLanguageKey("circle_effect"), "Growth");
            builder.add(ThaumoryCircleEffects.HEALING.toLanguageKey("circle_effect"), "Healing");
            builder.add(ThaumoryCircleEffects.ATTRACTION.toLanguageKey("circle_effect"), "Attraction");
            builder.add(ThaumoryCircleEffects.WEATHER.toLanguageKey("circle_effect"), "Weather");
            builder.add("hud.thaumory.core.essentia", "Essentia (up to %s each)");
            builder.add(ThaumoryBlocks.CHALK_LINE.get(), "Chalk Line");
            builder.add(ThaumoryBlocks.AMPLIFYING_PATTERN.get(), "Amplifying Pattern");
            builder.add(ThaumoryBlocks.EXTENDING_PATTERN.get(), "Extending Pattern");
            builder.add(ThaumoryBlocks.ECONOMIZING_PATTERN.get(), "Economizing Pattern");
            builder.add(ThaumoryBlocks.STABILIZING_PATTERN.get(), "Stabilizing Pattern");
            builder.add("message.thaumory.chapter.completed", "Chapter complete: %s");
            builder.add("message.thaumory.hint.appeared", "A new passage has appeared in the Arcane Codex");
            builder.add("codex.thaumory.chapter.complete", "✔ %s");
            builder.add("codex.thaumory.chapter.open", "◇ %s");
            builder.add("codex.thaumory.chapter.unlocks", "Unlocks: %s");
            builder.add("codex.thaumory.chapters.closed", "%s chapters are still closed.");
            builder.add("codex.thaumory.condition.met", "  ✔ %s");
            builder.add("codex.thaumory.condition.unmet", "  ・ %s");
            builder.add("codex.thaumory.condition.scan_item", "Scan %s");
            builder.add("codex.thaumory.condition.scan_tag", "Scan anything in %s");
            builder.add("codex.thaumory.condition.scan_count", "Scan %1$s kinds of items (%2$s/%1$s)");
            builder.add("codex.thaumory.condition.aspects_count", "Work out %1$s aspects (%2$s/%1$s)");
            builder.add("codex.thaumory.condition.aspects_list", "Work out %s");
            builder.add("codex.thaumory.condition.circle_success", "Make %1$s circles work (%2$s/%1$s)");
            builder.add("codex.thaumory.condition.circle_failure", "Record %1$s failed circles (%2$s/%1$s)");
            builder.add("codex.thaumory.condition.circle_effect_success", "Make %2$s circles of %1$s work (%3$s/%2$s)");
            builder.add("codex.thaumory.condition.circle_effect_failure", "Record %2$s failed circles of %1$s (%3$s/%2$s)");
            builder.add("chapter.thaumory.beginning", "Beginnings");
            builder.add("chapter.thaumory.beginning.text", "Everything is made of aspects. Peer at the things around you through the Arcane Loupe and their makeup slowly shows itself. What you see is written into this book.");
            builder.add("chapter.thaumory.aspects", "Aspects");
            builder.add("chapter.thaumory.aspects.text", "Look at a few things that share an aspect and its name comes clear. What is still nameless is written in unreadable glyphs.");
            builder.add("chapter.thaumory.crucible", "The Crucible");
            builder.add("chapter.thaumory.crucible.text", "A cauldron fitted with gold becomes a crucible. Fill it with water and light a fire beneath, and what you throw in melts into Essentia. What overflows, and what opposing aspects wear away, becomes Flux.");
            builder.add("chapter.thaumory.runes", "Runes");
            builder.add("chapter.thaumory.runes.text", "Stone steeped in Arcanum and Aqua becomes a blank rune; pour a jar's Essentia into it and it becomes that aspect's key. Clay steeped the same way gives chalk to draw circles with.");
            builder.add("chapter.thaumory.circles", "Magic Circles");
            builder.add("chapter.thaumory.circles.text", "Set two runes in a Core, and a third if you wish, then ring it with chalk lines. Touch it with the wand and the circle answers, if the combination is right.");
            builder.add("chapter.thaumory.flux", "Flux");
            builder.add("chapter.thaumory.flux.text", "A circle that fails to answer, or one that runs wild, spills Flux. Flux stagnates in the land, eats it away, and at last takes shape. There are said to be ways to clear it.");
            builder.add("chapter.thaumory.inquiry", "Inquiry");
            builder.add("chapter.thaumory.inquiry.text", "One who knows twelve aspects begins to hear what their combinations whisper.");
            builder.add("hint.thaumory.teleport", "Arcanum, bound with Aer, bends space.");
            builder.add("hint.thaumory.light", "Where Lux lies over Ignis, darkness gives way.");
            builder.add("hint.thaumory.purification", "Ordo and Lux clear what has stagnated.");
            builder.add("hint.thaumory.ward", "Vinculum and Ordo draw a border.");
            builder.add("hint.thaumory.growth", "Herba and Vita hurry the sprouting.");
            builder.add("hint.thaumory.healing", "Vita and Ordo close wounds.");
            builder.add("hint.thaumory.attraction", "Tempestas and Vinculum reel in what has fallen.");
            builder.add("hint.thaumory.weather", "Tempestas and Arcanum call the sky.");
            builder.add("codex.thaumory.tab.chapters", "Chapters");
            builder.add("codex.thaumory.tab.aspects", "Aspects");
            builder.add("codex.thaumory.tab.scanned", "Scanned");
            builder.add("codex.thaumory.tab.circles", "Circles");
            builder.add("codex.thaumory.tab.hints", "Hints");
            builder.add("codex.thaumory.hints.empty", "No hints have appeared yet.");
            builder.add("codex.thaumory.circles.empty", "No circles tried yet.");
            builder.add("codex.thaumory.circles.success", "Success");
            builder.add("codex.thaumory.circles.failure", "Failure");
            builder.add("codex.thaumory.aspects.count", "Worked out %s of %s");
            builder.add("codex.thaumory.scanned.count", "%s items scanned");
            builder.add("message.thaumory.scan.nothing", "Nothing to scan");
            builder.add("message.thaumory.scan.already", "%s is already scanned");
            builder.add("message.thaumory.scan.result", "Scanned %s: %s");
            builder.add("message.thaumory.scan.no_aspects", "Scanned %s: no aspects");
            builder.add("message.thaumory.scan.revealed", "Worked out a new aspect: %s");
            builder.add(ThaumoryBlocks.CRUCIBLE.get(), "Crucible");
            builder.add("hud.thaumory.crucible.water", "Water %s/%s");
            builder.add("hud.thaumory.crucible.boiling", "Boiling");
            builder.add("hud.thaumory.crucible.not_boiling", "Not boiling");
            builder.add("hud.thaumory.crucible.essentia", "Essentia %s/%s");
            builder.add("hud.thaumory.crucible.cancelling", "Opposites are cancelling out!");
        }
    }

    static final class Japanese extends ThaumoryLanguageProvider {
        Japanese(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, "ja_jp", registries);
        }

        @Override
        protected void translations(TranslationBuilder builder) {
            builder.add("tooltip.thaumory.aspects", "アスペクト:");
            builder.add(ThaumoryItems.ARCANE_LOUPE.get(), "魔術のルーペ");
            builder.add(ThaumoryItems.WAND.get(), "杖");
            builder.add("message.thaumory.wand.started", "陣が動き出した");
            builder.add("message.thaumory.wand.stopped", "陣が静まった");
            builder.add("message.thaumory.wand.triggered", "陣が発動した");
            builder.add("message.thaumory.wand.no_rings", "Core のまわりにリングが成立していない");
            builder.add("message.thaumory.wand.no_response", "陣は応えなかった");
            builder.add("message.thaumory.wand.no_essentia", "Core の Essentia が足りない");
            builder.add("message.thaumory.wand.no_target", "陣が働きかける先が見つからない");
            builder.add("message.thaumory.wand.misfired", "陣が乱れ、Flux が漏れ出した");
            builder.add("message.thaumory.wand.overloaded", "Flux にあふれた陣が暴発した！");
            builder.add("message.thaumory.circle.discovered", "新しい陣を見出した: %s");
            builder.add("hud.thaumory.flux.amount", "この辺りの Flux: %s");
            builder.add("hud.thaumory.block.unscanned", "未スキャン");
            builder.add("hud.thaumory.block.no_aspects", "アスペクトなし");
            builder.add("hud.thaumory.flux.stage.none", "平穏");
            builder.add("hud.thaumory.flux.stage.stagnation", "淀み");
            builder.add("hud.thaumory.flux.stage.erosion", "侵食");
            builder.add("hud.thaumory.flux.stage.manifestation", "顕現");
            builder.add("hud.thaumory.flux.stage.overload", "暴走");
            builder.add("codex.thaumory.flux_warning.stagnation", "この辺りは Flux で淀んでいる");
            builder.add("codex.thaumory.flux_warning.erosion", "この辺りの地が Flux に蝕まれている");
            builder.add("codex.thaumory.flux_warning.manifestation", "この辺りで Flux が形を得はじめている");
            builder.add("codex.thaumory.flux_warning.overload", "Flux があふれている。陣が暴発しかねない");
            builder.add("itemGroup.thaumory", "Thaumory");
            builder.add(ThaumoryItems.ARCANE_CODEX.get(), "魔術の書");
            builder.add(ThaumoryItems.BLANK_RUNE.get(), "空のルーン");
            builder.add(ThaumoryItems.RUNE.get(), "ルーン");
            builder.add("tooltip.thaumory.rune.aspect", "アスペクト: %s");
            builder.add("message.thaumory.rune.not_enough", "ルーンには 1 種類のアスペクトの Essentia が %s 必要");
            builder.add(ThaumoryBlocks.JAR.get(), "瓶");
            builder.add(ThaumoryItems.LABEL.get(), "ラベル");
            builder.add("message.thaumory.jar.cannot_label", "ラベルは中身が 1 種類の瓶にしか貼れない");
            builder.add("tooltip.thaumory.jar.label", "ラベル: %s");
            builder.add("tooltip.thaumory.jar.essentia", "Essentia %s");
            builder.add(ThaumoryItems.CHALK.get(), "チョーク");
            builder.add(ThaumoryItems.AMPLIFYING_CHALK.get(), "増幅のチョーク");
            builder.add(ThaumoryItems.EXTENDING_CHALK.get(), "延長のチョーク");
            builder.add(ThaumoryItems.ECONOMIZING_CHALK.get(), "節約のチョーク");
            builder.add(ThaumoryItems.STABILIZING_CHALK.get(), "安定のチョーク");
            builder.add(ThaumoryBlocks.CORE.get(), "陣の核");
            builder.add(ThaumoryBlocks.POLLUTED_SOIL.get(), "汚染された土");
            builder.add(ThaumoryBlocks.POLLUTED_STONE.get(), "汚染された石");
            builder.add(ThaumoryEntities.VOID_REMNANT.get(), "虚空の残滓");
            builder.add("message.thaumory.core.full", "ルーンのスロットは 3 つとも埋まっている");
            builder.add("hud.thaumory.core.runes", "ルーン:");
            builder.add("hud.thaumory.core.empty_slot", "—");
            builder.add("hud.thaumory.core.rings", "リング %s/%s");
            builder.add("hud.thaumory.core.node", "リング %s の%s: %s");
            builder.add("hud.thaumory.core.side.north", "北");
            builder.add("hud.thaumory.core.side.east", "東");
            builder.add("hud.thaumory.core.side.south", "南");
            builder.add("hud.thaumory.core.side.west", "西");
            builder.add("hud.thaumory.core.ignored", "節点の外の修飾 %s 個（効果なし）");
            builder.add("hud.thaumory.core.instability", "不安定度 %s/%s");
            builder.add("hud.thaumory.core.unstable", "不安定: 発動のたびに Flux が出るおそれがある");
            builder.add("hud.thaumory.core.running", "動作中");
            builder.add("hud.thaumory.core.stopped", "停止中");
            builder.add("hud.thaumory.core.upkeep.triggered", "1 回ごとに効果のルーンを %s ずつ、スロット 3 を 1");
            builder.add("hud.thaumory.core.upkeep.sustained", "%s 秒ごとに効果のルーンを 1 ずつ");
            builder.add("hud.thaumory.core.effect", "%sの陣");
            builder.add("hud.thaumory.core.unknown_circle", "未知の陣");
            builder.add("hud.thaumory.core.failed_circle", "失敗した組み合わせ");
            builder.add(ThaumoryCircleEffects.LIGHT.toLanguageKey("circle_effect"), "灯火");
            builder.add(ThaumoryCircleEffects.TELEPORT.toLanguageKey("circle_effect"), "テレポート");
            builder.add(ThaumoryCircleEffects.PURIFICATION.toLanguageKey("circle_effect"), "浄化");
            builder.add(ThaumoryCircleEffects.WARD.toLanguageKey("circle_effect"), "結界");
            builder.add(ThaumoryCircleEffects.GROWTH.toLanguageKey("circle_effect"), "成長");
            builder.add(ThaumoryCircleEffects.HEALING.toLanguageKey("circle_effect"), "治癒");
            builder.add(ThaumoryCircleEffects.ATTRACTION.toLanguageKey("circle_effect"), "引き寄せ");
            builder.add(ThaumoryCircleEffects.WEATHER.toLanguageKey("circle_effect"), "天候");
            builder.add("hud.thaumory.core.essentia", "Essentia（各 %s まで）");
            builder.add(ThaumoryBlocks.CHALK_LINE.get(), "チョークの線");
            builder.add(ThaumoryBlocks.AMPLIFYING_PATTERN.get(), "増幅の紋様");
            builder.add(ThaumoryBlocks.EXTENDING_PATTERN.get(), "延長の紋様");
            builder.add(ThaumoryBlocks.ECONOMIZING_PATTERN.get(), "節約の紋様");
            builder.add(ThaumoryBlocks.STABILIZING_PATTERN.get(), "安定の紋様");
            builder.add("message.thaumory.chapter.completed", "章を書き終えた: %s");
            builder.add("message.thaumory.hint.appeared", "魔術の書に新しい記述が現れた");
            builder.add("codex.thaumory.chapter.complete", "✔ %s");
            builder.add("codex.thaumory.chapter.open", "◇ %s");
            builder.add("codex.thaumory.chapter.unlocks", "開放: %s");
            builder.add("codex.thaumory.chapters.closed", "まだ開いていない章が %s ある。");
            builder.add("codex.thaumory.condition.met", "  ✔ %s");
            builder.add("codex.thaumory.condition.unmet", "  ・ %s");
            builder.add("codex.thaumory.condition.scan_item", "%s をスキャンする");
            builder.add("codex.thaumory.condition.scan_tag", "%s のどれかをスキャンする");
            builder.add("codex.thaumory.condition.scan_count", "アイテムを %1$s 種類スキャンする（%2$s/%1$s）");
            builder.add("codex.thaumory.condition.aspects_count", "アスペクトを %1$s 種類判明させる（%2$s/%1$s）");
            builder.add("codex.thaumory.condition.aspects_list", "%s を判明させる");
            builder.add("codex.thaumory.condition.circle_success", "陣を %1$s 通り動かす（%2$s/%1$s）");
            builder.add("codex.thaumory.condition.circle_failure", "陣の失敗を %1$s 通り記録する（%2$s/%1$s）");
            builder.add("codex.thaumory.condition.circle_effect_success", "%1$sを %2$s 通り動かす（%3$s/%2$s）");
            builder.add("codex.thaumory.condition.circle_effect_failure", "%1$sの失敗を %2$s 通り記録する（%3$s/%2$s）");
            builder.add("chapter.thaumory.beginning", "はじまり");
            builder.add("chapter.thaumory.beginning.text", "あらゆるものはアスペクトでできている。魔術のルーペで身の回りのものを覗き込めば、その成り立ちが少しずつ見えてくる。見たものはこの書に記される。");
            builder.add("chapter.thaumory.aspects", "アスペクト");
            builder.add("chapter.thaumory.aspects.text", "同じアスペクトを含むものを幾つか覗くと、その名が明らかになる。名の分からぬものは、読めない紋字で記される。");
            builder.add("chapter.thaumory.crucible", "るつぼ");
            builder.add("chapter.thaumory.crucible.text", "大釜に金を添えればるつぼになる。水を張り、下で火を焚けば、投げ入れたものは Essentia へ溶ける。溢れた分と、正反対のアスペクトが打ち消し合った分は Flux になる。");
            builder.add("chapter.thaumory.runes", "ルーン");
            builder.add("chapter.thaumory.runes.text", "石に Arcanum と Aqua を沁ませれば空のルーンとなる。瓶の Essentia を注げば、それはそのアスペクトの鍵になる。粘土に同じく沁ませれば、陣を描くチョークが得られる。");
            builder.add("chapter.thaumory.circles", "魔法陣");
            builder.add("chapter.thaumory.circles.text", "Core にルーンを二つ、望むなら三つ目を挿し、周りをチョークの線で囲む。杖で触れれば陣は応える――組み合わせが正しければ。");
            builder.add("chapter.thaumory.flux", "Flux");
            builder.add("chapter.thaumory.flux.text", "応えぬ陣、乱れた陣は Flux を吐く。Flux は土地に淀み、やがて地を蝕み、ついには形を得る。それを澄ませる術もあるという。");
            builder.add("chapter.thaumory.inquiry", "探究");
            builder.add("chapter.thaumory.inquiry.text", "十二のアスペクトを知る者は、その組み合わせの囁きを聞き分けはじめる。");
            builder.add("hint.thaumory.teleport", "Arcanum は、Aer と結びつくとき、空間を歪める。");
            builder.add("hint.thaumory.light", "Lux が Ignis に重なるとき、闇は退く。");
            builder.add("hint.thaumory.purification", "Ordo と Lux は、淀みを澄ませる。");
            builder.add("hint.thaumory.ward", "Vinculum と Ordo は、境を引く。");
            builder.add("hint.thaumory.growth", "Herba と Vita は、芽吹きを急かす。");
            builder.add("hint.thaumory.healing", "Vita と Ordo は、傷を塞ぐ。");
            builder.add("hint.thaumory.attraction", "Tempestas と Vinculum は、落ちたものを手繰り寄せる。");
            builder.add("hint.thaumory.weather", "Tempestas と Arcanum は、空を呼ぶ。");
            builder.add("codex.thaumory.tab.chapters", "章");
            builder.add("codex.thaumory.tab.aspects", "アスペクト");
            builder.add("codex.thaumory.tab.scanned", "スキャン済み");
            builder.add("codex.thaumory.tab.circles", "陣");
            builder.add("codex.thaumory.tab.hints", "ヒント");
            builder.add("codex.thaumory.hints.empty", "ヒントはまだ現れていない。");
            builder.add("codex.thaumory.circles.empty", "まだ陣を試していない。");
            builder.add("codex.thaumory.circles.success", "成功");
            builder.add("codex.thaumory.circles.failure", "失敗");
            builder.add("codex.thaumory.aspects.count", "%2$s 種のうち %1$s 種が判明");
            builder.add("codex.thaumory.scanned.count", "%s 種類をスキャン済み");
            builder.add("message.thaumory.scan.nothing", "スキャンできるものがない");
            builder.add("message.thaumory.scan.already", "%s はスキャン済み");
            builder.add("message.thaumory.scan.result", "%s をスキャンした: %s");
            builder.add("message.thaumory.scan.no_aspects", "%s をスキャンした: アスペクトなし");
            builder.add("message.thaumory.scan.revealed", "新しいアスペクトが判明した: %s");
            builder.add(ThaumoryBlocks.CRUCIBLE.get(), "るつぼ");
            builder.add("hud.thaumory.crucible.water", "水 %s/%s");
            builder.add("hud.thaumory.crucible.boiling", "沸騰中");
            builder.add("hud.thaumory.crucible.not_boiling", "沸騰していない");
            builder.add("hud.thaumory.crucible.essentia", "Essentia %s/%s");
            builder.add("hud.thaumory.crucible.cancelling", "正反対のアスペクトが打ち消し合っている！");
        }
    }
}
