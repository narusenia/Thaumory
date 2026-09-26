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

    /** What the fire focus shoots (requirements §17.7). */
    public static final RegistrySupplier<EntityType<FocusFireball>> FOCUS_FIREBALL = ENTITIES.register("focus_fireball",
            () -> EntityType.Builder.<FocusFireball>of(FocusFireball::new, MobCategory.MISC).sized(0.3125f, 0.3125f)
                    .clientTrackingRange(4).updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Thaumory.id("focus_fireball"))));

    private ThaumoryEntities() {}

    public static void register() {
        ENTITIES.register();
        EntityAttributeRegistry.register(VOID_REMNANT, VoidRemnant::createAttributes);
    }
}
