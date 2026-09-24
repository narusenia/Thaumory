package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.fabric.transfer.EssentiaStorage;
import one.nxeu.thaumory.fabric.transfer.EssentiaVariant;
import one.nxeu.thaumory.jar.JarContents;

/** Crucibles, jars and Cores as seen by another mod through the Transfer API (requirements §8.2). */
public class EssentiaStorageGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static Storage<EssentiaVariant> storage(GameTestHelper helper) {
        Storage<EssentiaVariant> storage = EssentiaStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), Direction.UP);
        helper.assertTrue(storage != null, "no essentia storage");
        return storage;
    }

    private static long insert(Storage<EssentiaVariant> storage, Aspect aspect, long amount) {
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = storage.insert(EssentiaVariant.of(aspect), amount, transaction);
            transaction.commit();
            return moved;
        }
    }

    private static long extract(Storage<EssentiaVariant> storage, Aspect aspect, long amount) {
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = storage.extract(EssentiaVariant.of(aspect), amount, transaction);
            transaction.commit();
            return moved;
        }
    }

    @GameTest
    public void crucibleGivesAnyAspectAndFillsToCapacity(GameTestHelper helper) {
        helper.setBlock(POS, ThaumoryBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(POS, CrucibleBlockEntity.class);
        crucible.setContents(AspectList.of(ThaumoryAspects.IGNIS, 10));
        Storage<EssentiaVariant> storage = storage(helper);
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(POS));
        double fluxBefore = Thaumory.flux().get(helper.getLevel(), chunk);

        helper.assertValueEqual(extract(storage, ThaumoryAspects.IGNIS, 4), 4L, "Ignis taken");
        helper.assertValueEqual(insert(storage, ThaumoryAspects.HERBA, 1000), (long) crucible.capacity() - 6, "Herba put in");
        helper.assertValueEqual(crucible.tank().contents(),
                AspectList.of(new AspectStack(ThaumoryAspects.IGNIS, 6), new AspectStack(ThaumoryAspects.HERBA, crucible.capacity() - 6)), "contents");
        helper.assertValueEqual(Thaumory.flux().get(helper.getLevel(), chunk), fluxBefore, "Flux");
        helper.succeed();
    }

    @GameTest
    public void abortedTransferLeavesTheContainerAlone(GameTestHelper helper) {
        helper.setBlock(POS, ThaumoryBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(POS, CrucibleBlockEntity.class);
        crucible.setContents(AspectList.of(ThaumoryAspects.IGNIS, 10));
        Storage<EssentiaVariant> storage = storage(helper);

        try (Transaction transaction = Transaction.openOuter()) {
            helper.assertValueEqual(storage.extract(EssentiaVariant.of(ThaumoryAspects.IGNIS), 7, transaction), 7L, "Ignis taken");
            long left = 0;
            for (StorageView<EssentiaVariant> view : storage.nonEmptyViews()) {
                left += view.getAmount();
            }
            helper.assertValueEqual(left, 3L, "Ignis seen during the transfer");
            helper.assertValueEqual(crucible.tank().contents(), AspectList.of(ThaumoryAspects.IGNIS, 10), "contents during the transfer");
        }
        helper.assertValueEqual(crucible.tank().contents(), AspectList.of(ThaumoryAspects.IGNIS, 10), "contents after aborting");
        helper.assertValueEqual(extract(storage, ThaumoryAspects.IGNIS, 100), 10L, "Ignis taken after aborting");
        helper.succeed();
    }

    @GameTest
    public void labeledJarMovesOnlyItsLabel(GameTestHelper helper) {
        helper.setBlock(POS, ThaumoryBlocks.JAR.get());
        JarBlockEntity jar = helper.getBlockEntity(POS, JarBlockEntity.class);
        jar.setContents(new JarContents(AspectList.of(ThaumoryAspects.IGNIS, 10), Optional.of(ThaumoryAspects.IGNIS)));
        Storage<EssentiaVariant> storage = storage(helper);

        helper.assertValueEqual(insert(storage, ThaumoryAspects.AQUA, 5), 0L, "Aqua put in");
        helper.assertValueEqual(insert(storage, ThaumoryAspects.IGNIS, 1000), (long) JarBlockEntity.capacity() - 10, "Ignis put in");
        helper.assertValueEqual(extract(storage, ThaumoryAspects.AQUA, 5), 0L, "Aqua taken");
        helper.assertValueEqual(jar.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, JarBlockEntity.capacity()), "contents");
        helper.succeed();
    }

    /** Kept apart from other tests, since it checks the Flux of its whole chunk. */
    @GameTest(padding = 24)
    public void unlabeledJarCancelsOppositesIntoFlux(GameTestHelper helper) {
        helper.setBlock(POS, ThaumoryBlocks.JAR.get());
        JarBlockEntity jar = helper.getBlockEntity(POS, JarBlockEntity.class);
        jar.setContents(new JarContents(AspectList.of(ThaumoryAspects.IGNIS, 10), Optional.empty()));
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(POS));
        Thaumory.flux().set(helper.getLevel(), chunk, 0);

        helper.assertValueEqual(insert(storage(helper), ThaumoryAspects.AQUA, 4), 4L, "Aqua put in");
        helper.assertValueEqual(jar.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, 6), "contents");
        helper.assertValueInBetween(7.9, Thaumory.flux().get(helper.getLevel(), chunk), 8.0, "Flux");
        helper.succeed();
    }

    @GameTest
    public void coreTakesOnlyItsRunesAndGivesNothing(GameTestHelper helper) {
        helper.setBlock(POS, ThaumoryBlocks.CIRCLE_CORE.get());
        CircleCoreBlockEntity core = helper.getBlockEntity(POS, CircleCoreBlockEntity.class);
        core.insert(ThaumoryAspects.LUX.id());
        core.insert(ThaumoryAspects.IGNIS.id());
        Storage<EssentiaVariant> storage = storage(helper);

        helper.assertValueEqual(insert(storage, ThaumoryAspects.LUX, 1000), (long) CircleCoreBlockEntity.capacity(), "Lux put in");
        helper.assertValueEqual(insert(storage, ThaumoryAspects.AQUA, 10), 0L, "Aqua put in");
        helper.assertValueEqual(extract(storage, ThaumoryAspects.LUX, 10), 0L, "Lux taken");
        helper.assertValueEqual(core.essentia(), AspectList.of(ThaumoryAspects.LUX, CircleCoreBlockEntity.capacity()), "contents");
        helper.succeed();
    }
}
