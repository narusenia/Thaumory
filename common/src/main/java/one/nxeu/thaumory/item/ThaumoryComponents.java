package one.nxeu.thaumory.item;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.jar.JarContents;

public final class ThaumoryComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Thaumory.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    /** A jar's Essentia and label, on the item and copied to and from the placed block. */
    public static final RegistrySupplier<DataComponentType<JarContents>> JAR_CONTENTS = COMPONENTS.register("jar_contents",
            () -> DataComponentType.<JarContents>builder()
                    .persistent(JarContents.codec(ThaumoryApi.aspects()))
                    .networkSynchronized(JarContents.streamCodec(ThaumoryApi.aspects()))
                    .build());

    private ThaumoryComponents() {}

    public static void register() {
        COMPONENTS.register();
    }
}
