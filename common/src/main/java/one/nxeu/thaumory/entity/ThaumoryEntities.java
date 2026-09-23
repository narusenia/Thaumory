package one.nxeu.thaumory.entity;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import one.nxeu.thaumory.Thaumory;

/** Thaumory's entities. */
public final class ThaumoryEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Thaumory.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<VoidRemnant>> VOID_REMNANT = ENTITIES.register("void_remnant",
            () -> EntityType.Builder.of(VoidRemnant::new, MobCategory.MONSTER).sized(0.4f, 0.8f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Thaumory.id("void_remnant"))));

    private ThaumoryEntities() {}

    public static void register() {
        ENTITIES.register();
        EntityAttributeRegistry.register(VOID_REMNANT, VoidRemnant::createAttributes);
    }
}
