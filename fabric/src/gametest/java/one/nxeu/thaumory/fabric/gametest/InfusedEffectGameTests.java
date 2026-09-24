package one.nxeu.thaumory.fabric.gametest;

import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.circle.effect.ThaumoryCircleEffects;
import one.nxeu.thaumory.infusion.Infusion;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.item.ThaumoryItems;

/** The life, defence and mining effects burnt into items (requirements §10.2). */
public class InfusedEffectGameTests {
    private static InfusionEffect effect(Identifier id) {
        return ThaumoryApi.infusionEffects().get(id).orElseThrow();
    }

    private static InfusionContext context(GameTestHelper helper, LivingEntity wearer, Identifier effect, int level) {
        ItemStack stack = new ItemStack(ThaumoryItems.ARCANE_IRON.sword().get());
        return InfusionRuntime.context(helper.getLevel(), wearer, stack, new Infusion(effect, level, Optional.empty(), 1));
    }

    @GameTest
    public void lightnessWornSpeedsTheWearerAndTakesAwayFallDamage(GameTestHelper helper) {
        Circles.floor(helper);
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(4, 2, 4));
        effect(ThaumoryCircleEffects.LIGHTNESS).tick(context(helper, cow, ThaumoryCircleEffects.LIGHTNESS, 2));
        MobEffectInstance speed = cow.getEffect(MobEffects.SPEED);
        helper.assertTrue(speed != null, "the wearer is not sped up");
        helper.assertValueEqual(speed.getAmplifier(), 1, "speed amplifier at level 2");
        float health = cow.getHealth();
        cow.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 6);
        helper.assertValueEqual(cow.getHealth(), health, "health after a fall");
        effect(ThaumoryCircleEffects.LIGHTNESS).stop(context(helper, cow, ThaumoryCircleEffects.LIGHTNESS, 2));
        cow.hurtServer(helper.getLevel(), helper.getLevel().damageSources().fall(), 6);
        helper.assertTrue(cow.getHealth() < health, "falls still do nothing once taken off");
        helper.succeed();
    }

    @GameTest
    public void breathAndNightSightWornWorkOnTheWearer(GameTestHelper helper) {
        Circles.floor(helper);
        Cow cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(4, 2, 4));
        effect(ThaumoryCircleEffects.BREATH).tick(context(helper, cow, ThaumoryCircleEffects.BREATH, 1));
        effect(ThaumoryCircleEffects.NIGHT_SIGHT).tick(context(helper, cow, ThaumoryCircleEffects.NIGHT_SIGHT, 1));
        helper.assertTrue(cow.hasEffect(MobEffects.WATER_BREATHING), "no water breathing");
        helper.assertTrue(cow.hasEffect(MobEffects.DOLPHINS_GRACE), "no dolphin's grace");
        helper.assertTrue(cow.getEffect(MobEffects.NIGHT_VISION).getDuration() > 200, "night vision is short enough to flicker");
        helper.succeed();
    }

    @GameTest
    public void bindingAndWitheringAfflictWhatTheMainHandHits(GameTestHelper helper) {
        Circles.floor(helper);
        Cow wearer = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 4));
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(6, 2, 4));
        effect(ThaumoryCircleEffects.BINDING).attack(context(helper, wearer, ThaumoryCircleEffects.BINDING, 1), husk);
        effect(ThaumoryCircleEffects.WITHERING).attack(context(helper, wearer, ThaumoryCircleEffects.WITHERING, 2), husk);
        MobEffectInstance slowness = husk.getEffect(MobEffects.SLOWNESS);
        helper.assertTrue(slowness != null, "not slowed");
        helper.assertValueEqual(slowness.getAmplifier(), 3, "slowness amplifier");
        helper.assertValueEqual(slowness.getDuration(), 60, "slowness ticks at level 1");
        helper.assertValueEqual(husk.getEffect(MobEffects.WITHER).getDuration(), 100, "wither ticks at level 2");
        helper.assertTrue(husk.hasEffect(MobEffects.WEAKNESS), "not weakened");
        helper.succeed();
    }

    @GameTest
    public void searingUsedBurnsHostileMobsAroundAndNeedsOne(GameTestHelper helper) {
        Circles.floor(helper);
        Cow wearer = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 4));
        InfusionEffect searing = effect(ThaumoryCircleEffects.SEARING);
        InfusionContext context = context(helper, wearer, ThaumoryCircleEffects.SEARING, 1);
        helper.assertFalse(searing.canUse(context), "usable with no hostile mob near");
        Husk husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(5, 2, 4));
        helper.assertTrue(searing.canUse(context), "not usable with a husk near");
        searing.use(context);
        helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "husk was not hurt");
        helper.assertTrue(husk.isOnFire(), "husk is not burning");
        helper.assertValueEqual(wearer.getHealth(), wearer.getMaxHealth(), "the user was hurt");
        helper.succeed();
    }

    @GameTest
    public void miningUsedDigsIntoTheFaceLookedAt(GameTestHelper helper) {
        Circles.floor(helper);
        for (int x = 5; x <= 8; x++) {
            for (int y = 2; y <= 6; y++) {
                for (int z = 1; z <= 7; z++) {
                    helper.setBlock(x, y, z, Blocks.STONE);
                }
            }
        }
        // Standing at x 3.5 and looking east at the wall.
        Husk user = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(3, 2, 4));
        user.setYRot(-90);
        user.setYHeadRot(-90);
        user.yRotO = -90;
        user.yHeadRotO = -90;
        user.setXRot(0);
        user.xRotO = 0;
        InfusionEffect mining = effect(ThaumoryCircleEffects.MINING);
        InfusionContext context = context(helper, user, ThaumoryCircleEffects.MINING, 1);
        helper.assertTrue(mining.canUse(context), "nothing to dig");
        mining.use(context);
        // A 3 × 3 face around the block looked at (x 5, eye height y 3), three deep.
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 3, 4));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(7, 2, 3));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(7, 4, 5));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(8, 3, 4));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(5, 5, 4));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(5, 3, 6));
        helper.assertItemEntityPresent(Items.COBBLESTONE);
        helper.succeed();
    }
}
