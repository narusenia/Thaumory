package one.nxeu.thaumory.fabric.transfer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.api.essentia.EssentiaContainer;
import one.nxeu.thaumory.essentia.PendingEssentia;

/**
 * An {@link EssentiaContainer} as a Fabric {@code Storage<EssentiaVariant>}. Moves change a
 * pending copy that rolls back with its transaction; the container sees the result only on the
 * final commit. Keep one per container ({@link EssentiaStorage} caches them) so two views of the
 * same container never hold different pending copies.
 */
final class ContainerStorage extends SnapshotParticipant<AspectList> implements Storage<EssentiaVariant> {
    private final EssentiaContainer container;
    private final PendingEssentia pending;
    /** Whether an open transaction has touched the copy; outside one, the container's own contents count. */
    private boolean inTransfer;

    ContainerStorage(EssentiaContainer container) {
        this.container = container;
        this.pending = new PendingEssentia(container);
    }

    /** The container's contents, unless a transaction is still moving things. */
    private void refresh() {
        if (!inTransfer) {
            pending.reset(container.contents());
        }
    }

    @Override
    public long insert(EssentiaVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) {
            return 0;
        }
        Aspect aspect = resource.aspect().orElseThrow();
        refresh();
        if (container.space(pending.contents(), aspect) <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        inTransfer = true;
        return pending.insert(aspect, clamp(maxAmount));
    }

    @Override
    public long extract(EssentiaVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) {
            return 0;
        }
        Aspect aspect = resource.aspect().orElseThrow();
        refresh();
        if (!container.canExtract(aspect) || pending.contents().amount(aspect) == 0) {
            return 0;
        }
        updateSnapshots(transaction);
        inTransfer = true;
        return pending.extract(aspect, clamp(maxAmount));
    }

    private static int clamp(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    @Override
    public Iterator<StorageView<EssentiaVariant>> iterator() {
        refresh();
        List<StorageView<EssentiaVariant>> views = new ArrayList<>();
        for (AspectStack stack : pending.contents().stacks()) {
            views.add(new View(EssentiaVariant.of(stack.aspect())));
        }
        return views.iterator();
    }

    @Override
    protected AspectList createSnapshot() {
        return pending.contents();
    }

    @Override
    protected void readSnapshot(AspectList snapshot) {
        pending.reset(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        inTransfer = false;
        if (pending.changed()) {
            container.update(pending.contents());
        }
    }

    @Override
    public void afterOuterClose(Transaction.Result result) {
        super.afterOuterClose(result);
        // An aborted transfer leaves nothing behind either.
        inTransfer = false;
    }

    /** One aspect's Essentia, read from the pending copy each time it is asked. */
    private final class View implements StorageView<EssentiaVariant> {
        private final EssentiaVariant resource;

        View(EssentiaVariant resource) {
            this.resource = resource;
        }

        @Override
        public long extract(EssentiaVariant requested, long maxAmount, TransactionContext transaction) {
            return requested.equals(resource) ? ContainerStorage.this.extract(requested, maxAmount, transaction) : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return false;
        }

        @Override
        public EssentiaVariant getResource() {
            return resource;
        }

        @Override
        public long getAmount() {
            refresh();
            return pending.contents().amount(resource.aspect().orElseThrow());
        }

        @Override
        public long getCapacity() {
            refresh();
            return getAmount() + container.space(pending.contents(), resource.aspect().orElseThrow());
        }
    }
}
