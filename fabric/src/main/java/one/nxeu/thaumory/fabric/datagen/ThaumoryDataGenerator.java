package one.nxeu.thaumory.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/** Run with {@code ./gradlew :fabric:runDatagen}; output lands in {@code common/src/main/generated}. */
public final class ThaumoryDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        exitWhenMainThreadEnds();

        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ThaumoryLanguageProvider.English::new);
        pack.addProvider(ThaumoryLanguageProvider.Japanese::new);
        pack.addProvider(ItemAspectProvider::new);
        pack.addProvider(FluxSettingsProvider::new);
        pack.addProvider(ScanSettingsProvider::new);
        pack.addProvider(CrucibleSettingsProvider::new);
        pack.addProvider(ThaumoryBlockTagProvider::new);
        pack.addProvider(ThaumoryBlockLootProvider::new);
        pack.addProvider(ThaumoryRecipeProvider::new);
        pack.addProvider(ThaumoryModelProvider::new);
    }

    /**
     * Datagen finishes by returning from main, but thread pools started by the dev runtime are
     * never shut down and keep the JVM (and the Gradle task) alive. Failures already exit with -1.
     */
    private static void exitWhenMainThreadEnds() {
        Thread main = Thread.currentThread();
        Thread watcher = new Thread(() -> {
            try {
                main.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.exit(0);
        }, "thaumory-datagen-exit");
        watcher.setDaemon(true);
        watcher.start();
    }
}
