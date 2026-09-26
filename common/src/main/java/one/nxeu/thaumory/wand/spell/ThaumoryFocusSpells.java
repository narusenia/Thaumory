package one.nxeu.thaumory.wand.spell;

import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.wand.FocusSpellRegistry;

/** What Thaumory's own wand foci cast (requirements §17.7), registered through the API. */
public final class ThaumoryFocusSpells {
    public static final Identifier LIGHT = Thaumory.id("light");
    public static final Identifier FIRE = Thaumory.id("fire");
    public static final Identifier FROST = Thaumory.id("frost");
    public static final Identifier LIGHTNING = Thaumory.id("lightning");
    public static final Identifier DIGGING = Thaumory.id("digging");
    public static final Identifier LEAP = Thaumory.id("leap");
    public static final Identifier EXCHANGE = Thaumory.id("exchange");

    private ThaumoryFocusSpells() {}

    public static void register(FocusSpellRegistry registry) {
        registry.register(LIGHT, new LightSpell());
        registry.register(FIRE, new FireSpell());
        registry.register(FROST, new FrostSpell());
        registry.register(LIGHTNING, new LightningSpell());
        registry.register(DIGGING, new DiggingSpell());
        registry.register(LEAP, new LeapSpell());
        registry.register(EXCHANGE, new ExchangeSpell());
    }
}
