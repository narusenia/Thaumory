package one.nxeu.thaumory.api.wand;

import one.nxeu.thaumory.api.Experimental;

/**
 * What a wand focus casts, registered in {@link one.nxeu.thaumory.api.ThaumoryApi#focusSpells()}. The
 * datapack's {@code thaumory/wand_focus} files say which item carries it, what one cast costs and how
 * long the wand then rests.
 */
@Experimental
public interface FocusSpell {
    /**
     * Called when the caster right-clicks with a wand that carries this spell. Call {@link
     * FocusContext#pay()} before changing anything; when it fails, change nothing and return false.
     *
     * @return whether anything came of it. False means nothing is paid and the wand does not rest.
     *     True without paying is a free cast (undoing an earlier one, say): the wand still rests.
     */
    boolean cast(FocusContext context);
}
