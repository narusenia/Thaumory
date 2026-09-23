package one.nxeu.thaumory.fabric.gametest;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CoreBlockEntity;
import one.nxeu.thaumory.block.crucible.CrucibleBlockEntity;
import one.nxeu.thaumory.block.jar.JarBlockEntity;
import one.nxeu.thaumory.block.pipe.PipeBlockEntity;
import one.nxeu.thaumory.block.pipe.ValveBlock;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.pipe.PipeNetworks;

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

    /** Kept apart from other tests, since it checks the Flux of its whole chunk. */
    @GameTest(maxTicks = 40, padding = 24)
    public void aBrokenPipeLeaksItsShareAsFlux(GameTestHelper helper) {
        List<BlockPos> line = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2), new BlockPos(4, 2, 2));
        for (BlockPos pos : line) {
            helper.setBlock(pos, ThaumoryBlocks.PIPE.get());
            helper.getBlockEntity(pos, PipeBlockEntity.class).setShare(AspectList.of(ThaumoryAspects.IGNIS, 3));
        }
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(line.get(1)));
        Thaumory.flux().set(helper.getLevel(), chunk, 0);
        PipeNetworks networks = PipeNetworks.of(helper.getLevel());
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> helper.destroyBlock(line.get(1)))
                .thenExecute(() -> helper.assertValueInBetween(2.9, Thaumory.flux().get(helper.getLevel(), chunk), 3.0, "Flux"))
                .thenIdle(2)
                .thenExecute(() -> {
                    int left = 0;
                    for (BlockPos pos : List.of(line.get(0), line.get(2))) {
                        left += networks.shareOf(helper.absolutePos(pos)).orElseThrow().total();
                    }
                    helper.assertValueEqual(left, 6, "Essentia left in the pipes");
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void aPumpDrawsFromACrucible(GameTestHelper helper) {
        helper.setBlock(FROM, ThaumoryBlocks.CRUCIBLE.get());
        CrucibleBlockEntity crucible = helper.getBlockEntity(FROM, CrucibleBlockEntity.class);
        crucible.setContents(AspectList.of(ThaumoryAspects.IGNIS, 20));
        JarBlockEntity to = jar(helper, TO, new JarContents(AspectList.empty(), Optional.of(ThaumoryAspects.IGNIS)));
        helper.setBlock(FROM.east(), ThaumoryBlocks.PUMP.get());
        for (int x = FROM.getX() + 2; x < TO.getX(); x++) {
            helper.setBlock(new BlockPos(x, 2, 2), ThaumoryBlocks.PIPE.get());
        }

        helper.assertBlockProperty(FROM.east(), BlockStateProperties.WEST, true);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(to.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, 20), "jar");
            helper.assertValueEqual(crucible.tank().contents(), AspectList.empty(), "Crucible");
        });
    }

    @GameTest(maxTicks = 100)
    public void aFilterPipeLetsOnlyItsAspectOut(GameTestHelper helper) {
        JarBlockEntity jar = jar(helper, FROM, new JarContents(
                AspectList.builder().add(ThaumoryAspects.HERBA, 10).add(ThaumoryAspects.TERRA, 10).build(), Optional.empty()));
        helper.setBlock(TO, ThaumoryBlocks.CORE.get());
        CoreBlockEntity core = helper.getBlockEntity(TO, CoreBlockEntity.class);
        core.insert(ThaumoryAspects.HERBA.id());
        core.insert(ThaumoryAspects.TERRA.id());
        helper.setBlock(FROM.east(), ThaumoryBlocks.FILTER_PIPE.get());
        helper.getBlockEntity(FROM.east(), PipeBlockEntity.class).setFilter(Optional.of(ThaumoryAspects.TERRA));
        for (int x = FROM.getX() + 2; x < TO.getX(); x++) {
            helper.setBlock(new BlockPos(x, 2, 2), ThaumoryBlocks.PIPE.get());
        }

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(core.essentia(), AspectList.of(ThaumoryAspects.TERRA, 10), "Core"))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertValueEqual(core.essentia(), AspectList.of(ThaumoryAspects.TERRA, 10), "Core later");
                    helper.assertValueEqual(jar.contents().aspects(), AspectList.of(ThaumoryAspects.HERBA, 10), "jar");
                })
                .thenSucceed();
    }

    @GameTest(maxTicks = 140)
    public void aPoweredValveParts(GameTestHelper helper) {
        JarBlockEntity from = jar(helper, FROM, new JarContents(AspectList.of(ThaumoryAspects.IGNIS, 8), Optional.empty()));
        JarBlockEntity to = jar(helper, TO, new JarContents(AspectList.empty(), Optional.of(ThaumoryAspects.IGNIS)));
        BlockPos valve = new BlockPos(3, 2, 2);
        BlockPos power = valve.above();
        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        helper.setBlock(FROM.east(), ThaumoryBlocks.PIPE.get());
        helper.setBlock(valve, ThaumoryBlocks.VALVE.get());
        helper.setBlock(TO.west(), ThaumoryBlocks.PIPE.get());

        helper.assertBlockProperty(valve, ValveBlock.POWERED, true);
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertValueEqual(to.contents().aspects(), AspectList.empty(), "labeled jar while closed");
                    helper.assertValueEqual(from.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, 8), "unlabeled jar while closed");
                })
                .thenExecute(() -> helper.setBlock(power, Blocks.AIR))
                .thenExecute(() -> helper.assertBlockProperty(valve, ValveBlock.POWERED, false))
                .thenWaitUntil(() -> helper.assertValueEqual(to.contents().aspects(), AspectList.of(ThaumoryAspects.IGNIS, 8), "labeled jar"))
                .thenSucceed();
    }
}
