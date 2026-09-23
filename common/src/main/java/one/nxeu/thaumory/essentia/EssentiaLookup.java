package one.nxeu.thaumory.essentia;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Finds Essentia storages in the world through the loader's own lookup, so other mods' count too. */
@FunctionalInterface
public interface EssentiaLookup {
    /** The storage of the block at {@code pos}, reached through its {@code side} face. */
    Optional<EssentiaHandle> find(Level level, BlockPos pos, Direction side);
}
