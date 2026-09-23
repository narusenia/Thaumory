package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
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
            builder.add(AspectText.UNKNOWN_KEY, "?");
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
            builder.add("hud.thaumory.flux.amount", "Flux here: %s");
            builder.add("hud.thaumory.block.unscanned", "Not scanned");
            builder.add("hud.thaumory.block.no_aspects", "No aspects");
            builder.add("hud.thaumory.flux.stage.none", "Calm");
            builder.add("hud.thaumory.flux.stage.stagnation", "Stagnation");
            builder.add("hud.thaumory.flux.stage.erosion", "Erosion");
            builder.add("hud.thaumory.flux.stage.manifestation", "Manifestation");
            builder.add("hud.thaumory.flux.stage.overload", "Overload");
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
            builder.add("codex.thaumory.tab.chapters", "Chapters");
            builder.add("codex.thaumory.tab.aspects", "Aspects");
            builder.add("codex.thaumory.tab.scanned", "Scanned");
            builder.add("codex.thaumory.tab.circles", "Circles");
            builder.add("codex.thaumory.tab.hints", "Hints");
            builder.add("codex.thaumory.chapters.empty", "No chapters completed yet.");
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
            builder.add(AspectText.UNKNOWN_KEY, "？");
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
            builder.add("hud.thaumory.flux.amount", "この辺りの Flux: %s");
            builder.add("hud.thaumory.block.unscanned", "未スキャン");
            builder.add("hud.thaumory.block.no_aspects", "アスペクトなし");
            builder.add("hud.thaumory.flux.stage.none", "平穏");
            builder.add("hud.thaumory.flux.stage.stagnation", "淀み");
            builder.add("hud.thaumory.flux.stage.erosion", "侵食");
            builder.add("hud.thaumory.flux.stage.manifestation", "顕現");
            builder.add("hud.thaumory.flux.stage.overload", "暴走");
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
            builder.add("codex.thaumory.tab.chapters", "章");
            builder.add("codex.thaumory.tab.aspects", "アスペクト");
            builder.add("codex.thaumory.tab.scanned", "スキャン済み");
            builder.add("codex.thaumory.tab.circles", "陣");
            builder.add("codex.thaumory.tab.hints", "ヒント");
            builder.add("codex.thaumory.chapters.empty", "完了した章はまだない。");
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
