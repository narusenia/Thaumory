package one.nxeu.thaumory.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.Thaumory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Crops do not grow where Flux has reached erosion. */
@Mixin(CropBlock.class)
abstract class CropBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void thaumory$stopInFlux(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Thaumory.flux().settings().effects().stopsCrops(Thaumory.flux().stage(level, ChunkPos.containing(pos)))) {
            ci.cancel();
        }
    }
}
