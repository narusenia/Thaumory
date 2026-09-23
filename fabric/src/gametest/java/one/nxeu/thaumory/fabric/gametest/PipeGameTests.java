package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.jar.JarContents;

/** Pipe networks carrying Essentia from lower to higher priority (requirements §8.2). */
public class PipeGameTests {
    private static final BlockPos FROM = new BlockPos(1, 2, 2);
    private static final BlockPos TO = new BlockPos(5, 2, 2);

    /** Pipes in a line between {@link #FROM} and {@link #TO}, placed like a player would so they join up. */
    private static void pipes(GameTestHelper helper) {
        for (int x = FROM.getX() + 1; x < TO.getX(); x++) {
            helper.setBlock(new BlockPos(x, 2, 2), ThaumoryBlocks.PIPE.get());
        }
    }

    private static JarBlockEntity jar(GameTestHelper helper, BlockPos pos, JarContents contents) {
        helper.setBlock(pos, ThaumoryBlocks.JAR.get());
        JarBlockEntity jar = helper.getBlockEntity(pos, JarBlockEntity.class);
        jar.setContents(contents);
        return jar;
    }

    @GameTest(maxTicks = 100)
    public void unlabeledJarFeedsACore(GameTestHelper helper) {
        JarBlockEntity jar = jar(helper, FROM, new JarContents(AspectList.of(ThaumoryAspects.IGNIS, 20), Optional.empty()));
        helper.setBlock(TO, ThaumoryBlocks.CORE.get());
        CoreBlockEntity core = helper.getBlockEntity(TO, CoreBlockEntity.class);
        core.insert(ThaumoryAspects.IGNIS.id());
        core.insert(ThaumoryAspects.AQUA.id());
        pipes(helper);

        helper.assertBlockProperty(FROM.east(), BlockStateProperties.WEST, true);
        helper.assertBlockProperty(TO.west(), BlockStateProperties.EAST, true);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(core.essentia(), AspectList.of(ThaumoryAspects.IGNIS, 20), "Core");
            helper.assertValueEqual(jar.contents().aspects(), AspectList.empty(), "jar");
        });
    }

    @GameTest(maxTicks = 100)
    public void unlabeledJarFillsALabeledOne(GameTestHelper helper) {
        JarBlockEntity from = jar(helper, FROM, new JarContents(AspectList.of(ThaumoryAspects.IGNIS, 20), Optional.empty()));
        JarBlockEntity to = jar(helper, TO, new JarContents(AspectList.empty(), Optional.of(ThaumoryAspects.IGNIS)));
        pipes(helper);

        helper.succeedWhen(() -> {
            helper.assertValueEqual(to.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, 20), "labeled jar");
            helper.assertValueEqual(from.contents().aspects(), AspectList.empty(), "unlabeled jar");
        });
    }

    @GameTest(maxTicks = 60)
    public void nothingFlowsBetweenUnlabeledJars(GameTestHelper helper) {
        JarBlockEntity from = jar(helper, FROM, new JarContents(AspectList.of(ThaumoryAspects.IGNIS, 20), Optional.empty()));
        JarBlockEntity to = jar(helper, TO, new JarContents(AspectList.empty(), Optional.empty()));
        pipes(helper);

        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(from.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, 20), "first jar");
            helper.assertValueEqual(to.contents().aspects(), AspectList.empty(), "second jar");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void pipesLeaveCruciblesAlone(GameTestHelper helper) {
        helper.setBlock(FROM, ThaumoryBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(FROM, CrucibleBlockEntity.class);
        crucible.setContents(AspectList.of(ThaumoryAspects.IGNIS, 20));
        JarBlockEntity to = jar(helper, TO, new JarContents(AspectList.empty(), Optional.of(ThaumoryAspects.IGNIS)));
        pipes(helper);

        helper.assertBlockProperty(FROM.east(), BlockStateProperties.WEST, false);
        helper.runAtTickTime(50, () -> {
            helper.assertValueEqual(crucible.tank().contents(), AspectList.of(ThaumoryAspects.IGNIS, 20), "Crucible");
            helper.assertValueEqual(to.contents().aspects(), AspectList.empty(), "jar");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 100)
    public void aNetworkJoinsUpWhenTheGapIsFilled(GameTestHelper helper) {
        JarBlockEntity from = jar(helper, FROM, new JarContents(AspectList.of(ThaumoryAspects.IGNIS, 8), Optional.empty()));
        JarBlockEntity to = jar(helper, TO, new JarContents(AspectList.empty(), Optional.of(ThaumoryAspects.IGNIS)));
        helper.setBlock(FROM.east(), ThaumoryBlocks.PIPE.get());
        helper.setBlock(TO.west(), ThaumoryBlocks.PIPE.get());
        helper.startSequence()
                .thenIdle(30)
                .thenExecute(() -> helper.assertValueEqual(to.contents().aspects(), AspectList.empty(), "labeled jar before joining"))
                .thenExecute(() -> helper.setBlock(new BlockPos(3, 2, 2), ThaumoryBlocks.PIPE.get()))
                .thenWaitUntil(() -> helper.assertValueEqual(to.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, 8), "labeled jar"))
                .thenExecute(() -> helper.assertValueEqual(from.contents().aspects(), AspectList.empty(), "unlabeled jar"))
                .thenSucceed();
    }
}
