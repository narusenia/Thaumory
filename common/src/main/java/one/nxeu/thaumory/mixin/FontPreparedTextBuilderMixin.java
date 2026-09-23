package one.nxeu.thaumory.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.font.TextRenderable;
import one.nxeu.thaumory.client.text.TextEffectPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Draws glyphs in Thaumory's effect fonts with their effect. */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
abstract class FontPreparedTextBuilderMixin {
    @ModifyExpressionValue(
            method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;createGlyph(FFIILnet/minecraft/network/chat/Style;FF)Lnet/minecraft/client/gui/font/TextRenderable$Styled;"))
    private TextRenderable.Styled thaumory$applyEffect(TextRenderable.Styled glyph) {
        return TextEffectPipelines.wrap(glyph);
    }
}
