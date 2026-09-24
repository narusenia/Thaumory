package one.nxeu.thaumory.sound;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import one.nxeu.thaumory.Thaumory;

/** Thaumory's own sounds; which files each plays is in {@code assets/thaumory/sounds.json} (datagen). */
public final class ThaumorySounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Thaumory.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> CIRCLE_ACTIVATE = register("block.circle_core.activate");
    public static final RegistrySupplier<SoundEvent> CIRCLE_DEACTIVATE = register("block.circle_core.deactivate");
    public static final RegistrySupplier<SoundEvent> CIRCLE_INFUSE = register("block.circle_core.infuse");
    /** Flux let out by a mistake: a misfire, instability, a failed infusion. */
    public static final RegistrySupplier<SoundEvent> CIRCLE_FLUX = register("block.circle_core.flux");
    /** An item melting into the Crucible. */
    public static final RegistrySupplier<SoundEvent> CRUCIBLE_MELT = register("block.crucible.melt");
    public static final RegistrySupplier<SoundEvent> CRUCIBLE_ALCHEMY = register("block.crucible.alchemy");
    /** A scan that reveals an aspect. */
    public static final RegistrySupplier<SoundEvent> ASPECT_REVEALED = register("item.arcane_loupe.reveal");
    public static final RegistrySupplier<SoundEvent> JAR_USE = register("item.jar.use");
    public static final RegistrySupplier<SoundEvent> RUNE_MADE = register("item.rune.made");
    public static final RegistrySupplier<SoundEvent> TRANSCRIBE = register("item.transcript.write");
    public static final RegistrySupplier<SoundEvent> CODEX_PAGE = register("item.arcane_codex.page");
    /** A chapter completed or a new circle found. */
    public static final RegistrySupplier<SoundEvent> DISCOVERY = register("research.discovery");

    // A block's sounds are fixed when it is made, before registration, so these events exist up front.
    private static final SoundEvent JAR_BREAK_EVENT = SoundEvent.createVariableRangeEvent(Thaumory.id("block.jar.break"));
    private static final SoundEvent JAR_PLACE_EVENT = SoundEvent.createVariableRangeEvent(Thaumory.id("block.jar.place"));
    public static final RegistrySupplier<SoundEvent> JAR_BREAK = SOUNDS.register("block.jar.break", () -> JAR_BREAK_EVENT);
    public static final RegistrySupplier<SoundEvent> JAR_PLACE = SOUNDS.register("block.jar.place", () -> JAR_PLACE_EVENT);
    /** The jar's own sounds for placing and breaking it; glass for the rest. */
    public static final SoundType JAR = new SoundType(1.0f, 1.0f, JAR_BREAK_EVENT, SoundEvents.GLASS_STEP, JAR_PLACE_EVENT,
            SoundEvents.GLASS_HIT, SoundEvents.GLASS_FALL);

    private ThaumorySounds() {}

    private static RegistrySupplier<SoundEvent> register(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Thaumory.id(path)));
    }

    public static void register() {
        SOUNDS.register();
    }
}
