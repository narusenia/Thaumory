package one.nxeu.thaumory.circle.effect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.aspect.ThaumoryAspects;

/**
 * Light on an item, passive. With slot 3 empty, an invisible light follows the wearer through dark
 * places. With Umbra, whatever the wearer hits with it is left in darkness.
 */
final class LightInfusion implements InfusionEffect {
    private static final int DARK = 7;
    private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
    /** The light each wearer has placed, so it can be taken back when they move on. */
    private static final Map<UUID, GlobalPos> PLACED = new ConcurrentHashMap<>();

    @Override
    public int period() {
        return 10;
    }

    private static boolean umbra(InfusionContext context) {
        return context.parameter().filter(ThaumoryAspects.UMBRA::equals).isPresent();
    }

    @Override
    public void tick(InfusionContext context) {
        if (umbra(context)) {
            return;
        }
        ServerLevel level = context.level();
        LivingEntity wearer = context.wearer();
        BlockPos head = BlockPos.containing(wearer.getEyePosition());
        GlobalPos here = GlobalPos.of(level.dimension(), head);
        GlobalPos previous = PLACED.get(wearer.getUUID());
        if (here.equals(previous)) {
            return;
        }
        clear(level, wearer.getUUID());
        if (level.getBlockState(head).isAir() && level.getBrightness(LightLayer.BLOCK, head) <= DARK) {
            level.setBlock(head, LIGHT, Block.UPDATE_ALL);
            PLACED.put(wearer.getUUID(), here);
        }
    }

    @Override
    public void stop(InfusionContext context) {
        clear(context.level(), context.wearer().getUUID());
    }

    /** Takes the wearer's light back if it is still there. */
    private static void clear(ServerLevel anyLevel, UUID wearer) {
        GlobalPos placed = PLACED.remove(wearer);
        if (placed == null) {
            return;
        }
        ServerLevel level = anyLevel.getServer().getLevel(placed.dimension());
        if (level != null && level.isLoaded(placed.pos()) && level.getBlockState(placed.pos()).equals(LIGHT)) {
            level.setBlock(placed.pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void attack(InfusionContext context, LivingEntity target) {
        if (umbra(context)) {
            int seconds = (int) Math.round(context.setting("darkness_seconds", 3) + context.infusionLevel());
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, seconds * 20, 0));
        }
    }
}
