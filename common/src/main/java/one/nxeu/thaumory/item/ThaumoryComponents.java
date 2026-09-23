package one.nxeu.thaumory.item;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.knowledge.Transcript;

public final class ThaumoryComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Thaumory.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    /** A jar's Essentia and label, on the item and copied to and from the placed block. */
    public static final RegistrySupplier<DataComponentType<JarContents>> JAR_CONTENTS = COMPONENTS.register("jar_contents",
            () -> DataComponentType.<JarContents>builder()
                    .persistent(JarContents.codec(ThaumoryApi.aspects()))
                    .networkSynchronized(JarContents.streamCodec(ThaumoryApi.aspects()))
                    .build());

    /**
     * A rune's aspect. Kept as an id so a rune whose aspect's addon was removed keeps it, as
     * knowledge does.
     */
    public static final RegistrySupplier<DataComponentType<Identifier>> RUNE_ASPECT = COMPONENTS.register("rune_aspect",
            () -> DataComponentType.<Identifier>builder()
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC)
                    .build());

    /** What a transcript teaches whoever reads it. */
    public static final RegistrySupplier<DataComponentType<Transcript>> TRANSCRIPT = COMPONENTS.register("transcript",
            () -> DataComponentType.<Transcript>builder()
                    .persistent(Transcript.CODEC)
                    .networkSynchronized(Transcript.STREAM_CODEC)
                    .build());

    private ThaumoryComponents() {}

    public static void register() {
        COMPONENTS.register();
    }
}
