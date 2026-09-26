package one.nxeu.thaumory.wand.spell;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;
import one.nxeu.thaumory.entity.FocusFireball;

/** Ignis (requirements §17.7): a fireball that burns what it hits and leaves the ground alone. */
final class FireSpell implements FocusSpell {
    @Override
    public boolean cast(FocusContext context) {
        if (!context.pay()) {
            return false;
        }
        Player caster = context.caster();
        FocusFireball fireball = new FocusFireball(context.level(), caster, (float) (context.setting("damage", 4) * context.power()),
                (int) context.setting("burn_seconds", 4));
        fireball.setPos(SpellTargets.tip(caster));
        fireball.shootFromRotation(caster, caster.getXRot(), caster.getYHeadRot(), 0, (float) context.setting("speed", 1.5), 0);
        context.level().addFreshEntity(fireball);
        context.level().playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8f, 1.2f);
        return true;
    }
}
