package one.nxeu.thaumory.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.pipe.PipeBlockEntity;

/** Colors a filter pipe's band with its filter's aspect; a filter pipe with no filter keeps it white. */
final class FilterPipeTint implements BlockTintSource {
    private static final int NONE = 0xFFFFFFFF;

    private FilterPipeTint() {}

    static void register() {
        ColorHandlerRegistry.registerBlockColors(new FilterPipeTint(), ThaumoryBlocks.FILTER_PIPE);
    }

    @Override
    public int color(BlockState state) {
        return NONE;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PipeBlockEntity pipe
                ? pipe.filter().map(aspect -> 0xFF000000 | aspect.color()).orElse(NONE)
                : NONE;
    }
}
