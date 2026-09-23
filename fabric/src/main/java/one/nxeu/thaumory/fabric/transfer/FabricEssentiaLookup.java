package one.nxeu.thaumory.fabric.transfer;

import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.essentia.EssentiaHandle;
import one.nxeu.thaumory.essentia.EssentiaLookup;

/** Finds storages through {@link EssentiaStorage#SIDED}, so other mods' storages join pipes too. */
public final class FabricEssentiaLookup implements EssentiaLookup {
    @Override
    public Optional<EssentiaHandle> find(Level level, BlockPos pos, Direction side) {
        return Optional.ofNullable(EssentiaStorage.SIDED.find(level, pos, side)).map(Handle::new);
    }

    /** Each move is a transaction of its own, committed at once. */
    private record Handle(Storage<EssentiaVariant> storage) implements EssentiaHandle {
        @Override
        public AspectList contents() {
            AspectList.Builder contents = AspectList.builder();
            for (StorageView<EssentiaVariant> view : storage.nonEmptyViews()) {
                view.getResource().aspect().ifPresent(aspect -> contents.add(aspect, (int) Math.min(Integer.MAX_VALUE, view.getAmount())));
            }
            return contents.build();
        }

        @Override
        public int space(Aspect aspect) {
            if (Transaction.isOpen()) {
                return 0;
            }
            try (Transaction simulation = Transaction.openOuter()) {
                return (int) storage.insert(EssentiaVariant.of(aspect), Integer.MAX_VALUE, simulation);
            }
        }

        @Override
        public int insert(Aspect aspect, int max) {
            if (Transaction.isOpen() || max <= 0) {
                return 0;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long moved = storage.insert(EssentiaVariant.of(aspect), max, transaction);
                transaction.commit();
                return (int) moved;
            }
        }

        @Override
        public int extract(Aspect aspect, int max) {
            if (Transaction.isOpen() || max <= 0) {
                return 0;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long moved = storage.extract(EssentiaVariant.of(aspect), max, transaction);
                transaction.commit();
                return (int) moved;
            }
        }
    }
}
