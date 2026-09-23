package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
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
            builder.add(ThaumoryItems.ARCANE_CODEX.get(), "Arcane Codex");
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
            builder.add(ThaumoryItems.ARCANE_CODEX.get(), "魔術の書");
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
        }
    }
}
