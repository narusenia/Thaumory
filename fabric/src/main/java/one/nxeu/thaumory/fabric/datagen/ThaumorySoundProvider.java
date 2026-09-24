package one.nxeu.thaumory.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder;
import net.fabricmc.fabric.api.client.datagen.v1.builder.SoundTypeBuilder.RegistrationBuilder;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricSoundsProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.sounds.SoundEvent;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.sound.ThaumorySounds;

/** Writes {@code assets/thaumory/sounds.json}: the files under {@code sounds/} behind each sound. */
final class ThaumorySoundProvider extends FabricSoundsProvider {
    ThaumorySoundProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    /** Most sounds play at this volume; the files themselves are mastered loud. */
    private static final float VOLUME = 0.6f;

    @Override
    protected void configure(HolderLookup.Provider registries, SoundExporter exporter) {
        add(exporter, ThaumorySounds.CIRCLE_ACTIVATE.get(), "circle/activate", VOLUME);
        add(exporter, ThaumorySounds.CIRCLE_DEACTIVATE.get(), "circle/deactivate", VOLUME);
        add(exporter, ThaumorySounds.CIRCLE_INFUSE.get(), "circle/infuse", VOLUME);
        add(exporter, ThaumorySounds.CIRCLE_FLUX.get(), "circle/flux", VOLUME);
        add(exporter, ThaumorySounds.CRUCIBLE_MELT.get(), "crucible/melt", VOLUME);
        add(exporter, ThaumorySounds.CRUCIBLE_ALCHEMY.get(), "crucible/alchemy", VOLUME);
        add(exporter, ThaumorySounds.ASPECT_REVEALED.get(), "research/reveal", 0.8f);
        add(exporter, ThaumorySounds.RUNE_MADE.get(), "rune/made", VOLUME);
        add(exporter, ThaumorySounds.TRANSCRIBE.get(), "research/transcribe", VOLUME);
        add(exporter, ThaumorySounds.DISCOVERY.get(), "research/discover", VOLUME);
        for (SoundEvent jar : new SoundEvent[] {ThaumorySounds.JAR_USE.get(), ThaumorySounds.JAR_PLACE.get(), ThaumorySounds.JAR_BREAK.get()}) {
            exporter.add(jar, SoundTypeBuilder.of(jar).sound(RegistrationBuilder.ofFile(Thaumory.id("jar/use")).volume(0.4f), 4));
        }
        // Quiet already.
        exporter.add(ThaumorySounds.CODEX_PAGE.get(), SoundTypeBuilder.of(ThaumorySounds.CODEX_PAGE.get())
                .sound(RegistrationBuilder.ofFile(Thaumory.id("codex/page")), 2));
    }

    private static void add(SoundExporter exporter, SoundEvent event, String file, float volume) {
        exporter.add(event, SoundTypeBuilder.of(event).sound(RegistrationBuilder.ofFile(Thaumory.id(file)).volume(volume)));
    }

    @Override
    public String getName() {
        return "Thaumory sounds";
    }
}
