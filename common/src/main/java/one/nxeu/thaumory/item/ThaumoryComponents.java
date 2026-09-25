package one.nxeu.thaumory.item;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectCodecs;
import one.nxeu.thaumory.infusion.Infusions;
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

    /** The circle effects burnt into an item by infusion. */
    public static final RegistrySupplier<DataComponentType<Infusions>> INFUSIONS = COMPONENTS.register("infusions",
            () -> DataComponentType.<Infusions>builder()
                    .persistent(Infusions.CODEC)
                    .networkSynchronized(Infusions.STREAM_CODEC)
                    .build());

    /** The Essentia an item with an active infusion keeps for it (requirements §10.2). */
    public static final RegistrySupplier<DataComponentType<AspectList>> STORED_ESSENTIA = COMPONENTS.register("stored_essentia",
            () -> DataComponentType.<AspectList>builder()
                    .persistent(AspectCodecs.aspectList(ThaumoryApi.aspects()))
                    .networkSynchronized(ByteBufCodecs.fromCodec(AspectCodecs.aspectList(ThaumoryApi.aspects())))
                    .build());

    /** The Flux sealed in one Flux crystal, given back to the chunk when it breaks (requirements §17.5). */
    public static final RegistrySupplier<DataComponentType<Integer>> SEALED_FLUX = COMPONENTS.register("sealed_flux",
            () -> DataComponentType.<Integer>builder()
                    .persistent(ExtraCodecs.POSITIVE_INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ThaumoryComponents() {}

    public static void register() {
        COMPONENTS.register();
    }
}
