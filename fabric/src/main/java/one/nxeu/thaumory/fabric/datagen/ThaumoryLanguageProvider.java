package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
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
            builder.add("message.thaumory.scan.nothing", "Nothing to scan");
            builder.add("message.thaumory.scan.already", "%s is already scanned");
            builder.add("message.thaumory.scan.result", "Scanned %s: %s");
            builder.add("message.thaumory.scan.no_aspects", "Scanned %s: no aspects");
            builder.add("message.thaumory.scan.revealed", "Worked out a new aspect: %s");
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
            builder.add("message.thaumory.scan.nothing", "スキャンできるものがない");
            builder.add("message.thaumory.scan.already", "%s はスキャン済み");
            builder.add("message.thaumory.scan.result", "%s をスキャンした: %s");
            builder.add("message.thaumory.scan.no_aspects", "%s をスキャンした: アスペクトなし");
            builder.add("message.thaumory.scan.revealed", "新しいアスペクトが判明した: %s");
        }
    }
}
