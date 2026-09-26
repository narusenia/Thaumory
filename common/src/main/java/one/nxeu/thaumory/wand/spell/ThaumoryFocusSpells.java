package one.nxeu.thaumory.wand.spell;

import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.wand.FocusSpellRegistry;

/** What Thaumory's own wand foci cast (requirements §17.7), registered through the API. */
public final class ThaumoryFocusSpells {
    public static final Identifier LIGHT = Thaumory.id("light");

    private ThaumoryFocusSpells() {}

    public static void register(FocusSpellRegistry registry) {
        registry.register(LIGHT, new LightSpell());
    }
}
