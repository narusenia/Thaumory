package one.nxeu.thaumory.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.circle.effect.SafeguardEffect;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps explosions from breaking (or setting fire to) blocks inside a safeguard circle. */
@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Vec3 center;

    @ModifyExpressionValue(method = "explode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
    private List<BlockPos> thaumory$spareSafeguarded(List<BlockPos> blocks) {
        return SafeguardEffect.spare(level, center, blocks);
    }
}
