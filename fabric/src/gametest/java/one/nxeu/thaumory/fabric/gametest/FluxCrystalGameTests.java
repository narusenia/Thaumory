package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.aspect.ThaumoryAspects;
import one.nxeu.thaumory.block.ThaumoryBlocks;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity.StartResult;
import one.nxeu.thaumory.item.ThaumoryItems;

/**
 * The containment circle and the Flux crystals it makes (requirements §17.5). Each test checks the
 * Flux of a whole chunk, so they are kept apart from the others.
 */
public class FluxCrystalGameTests {
    private static final BlockPos CORE = new BlockPos(4, 2, 4);
    private static final BlockPos SPOT = new BlockPos(2, 2, 2);

    @GameTest(maxTicks = 200, padding = 24)
    public void containmentSealsFluxIntoCrystals(GameTestHelper helper) {
        Circles.floor(helper);
        CircleCoreBlockEntity core = Circles.build(helper, CORE, ThaumoryAspects.VINCULUM, ThaumoryAspects.CHAOS);
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(CORE));
        Thaumory.flux().set(helper.getLevel(), chunk, 25);
        helper.assertValueEqual(core.start(Optional.empty()), StartResult.STARTED, "start");
        helper.succeedWhen(() -> {
            helper.assertItemEntityPresent(ThaumoryItems.FLUX_CRYSTAL.get(), CORE, 2.0);
            helper.assertTrue(Thaumory.flux().get(helper.getLevel(), chunk) <= 15, "Flux was not drawn in");
        });
    }

    @GameTest(maxTicks = 100, padding = 24)
    public void crystalBreaksInWater(GameTestHelper helper) {
        helper.setBlock(SPOT.below(), Blocks.STONE);
        helper.setBlock(SPOT, Blocks.WATER);
        dropsBackItsFlux(helper);
    }

    @GameTest(maxTicks = 200, padding = 24)
    public void crystalBreaksInLava(GameTestHelper helper) {
        helper.setBlock(SPOT.below(), Blocks.STONE);
        helper.setBlock(SPOT, Blocks.LAVA);
        dropsBackItsFlux(helper);
    }

    /** Melting lets the sealed Flux out on top of the aspects. */
    @GameTest(maxTicks = 400, padding = 24)
    public void meltingCrystalLetsOutItsFlux(GameTestHelper helper) {
        helper.setBlock(SPOT.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(SPOT, ThaumoryBlocks.CRUCIBLE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.useBlock(SPOT, player);
        dropsBackItsFlux(helper);
    }

    private static void dropsBackItsFlux(GameTestHelper helper) {
        ChunkPos chunk = ChunkPos.containing(helper.absolutePos(SPOT));
        Thaumory.flux().set(helper.getLevel(), chunk, 0);
        Vec3 at = helper.absoluteVec(Vec3.atCenterOf(SPOT));
        helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(ThaumoryItems.FLUX_CRYSTAL.get())));
        helper.succeedWhen(() -> {
            helper.assertItemEntityNotPresent(ThaumoryItems.FLUX_CRYSTAL.get(), SPOT, 2.0);
            // Flux decays slowly all the time, so a little under 10 is still the crystal's 10.
            helper.assertValueInBetween(9.9, Thaumory.flux().get(helper.getLevel(), chunk), 10.0, "Flux");
        });
    }
}
