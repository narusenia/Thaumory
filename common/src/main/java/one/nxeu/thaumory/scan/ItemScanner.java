package one.nxeu.thaumory.scan;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.aspect.AspectText;
import one.nxeu.thaumory.aspect.data.ItemAspects;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** Scanning with the Arcane Loupe, on the server. */
public final class ItemScanner {
    private static volatile ScanSettings settings = ScanSettings.DEFAULT;

    private ItemScanner() {}

    public static void updateSettings(ScanSettings newSettings) {
        settings = newSettings;
    }

    /** Scans blocks instead of opening them. Runs on both sides so the client skips the block's interaction too. */
    public static void register() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown() || !player.getItemInHand(hand).is(ThaumoryItems.ARCANE_LOUPE.get())) {
                return EventResult.pass();
            }
            if (player instanceof ServerPlayer serverPlayer) {
                scan(serverPlayer);
            }
            return EventResult.interruptTrue();
        });
    }

    /** Scans what the player is aiming at with the loupe. */
    public static void scan(ServerPlayer player) {
        target(player).ifPresentOrElse(item -> scan(player, item),
                () -> player.sendOverlayMessage(Component.translatable("message.thaumory.scan.nothing")));
    }

    /** Records {@code item} as scanned by the player, reveals aspects, and tells the player what they learned. */
    public static void scan(ServerPlayer player, Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);

        Scanning.Result result = Scanning.scanItem(Thaumory.knowledge().get(player), id,
                scanned -> BuiltInRegistries.ITEM.getOptional(scanned).map(ItemAspects::get).orElse(AspectList.empty()),
                settings);
        PlayerKnowledge knowledge = Thaumory.knowledge().update(player, before -> result.knowledge());

        Component name = item.getDefaultInstance().getHoverName();
        if (!result.newlyScanned() && result.revealed().isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.thaumory.scan.already", name));
            return;
        }
        AspectList aspects = ItemAspects.get(item);
        player.sendSystemMessage(aspects.isEmpty()
                ? Component.translatable("message.thaumory.scan.no_aspects", name)
                : Component.translatable("message.thaumory.scan.result", name,
                        AspectText.list(aspects, aspect -> knowledge.knowsAspect(aspect.id()))));
        result.revealed().forEach(aspect -> player.sendSystemMessage(
                Component.translatable("message.thaumory.scan.revealed", AspectText.name(aspect, true))));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                result.revealed().isEmpty() ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    /** An item lying in reach, else the block looked at, else the item in the off hand. */
    private static Optional<Item> target(ServerPlayer player) {
        double reach = player.blockInteractionRange();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        Vec3 end = eye.add(look.scale(reach));

        HitResult block = player.pick(reach, 1, false);
        double blockDistance = block.getType() == HitResult.Type.MISS ? reach * reach : eye.distanceToSqr(block.getLocation());
        AABB area = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, end, area,
                candidate -> candidate instanceof ItemEntity && candidate.isAlive(), blockDistance);
        if (entity != null && entity.getEntity() instanceof ItemEntity itemEntity) {
            return Optional.of(itemEntity.getItem().getItem());
        }
        if (block instanceof BlockHitResult hit && block.getType() == HitResult.Type.BLOCK) {
            Item item = player.level().getBlockState(hit.getBlockPos()).getBlock().asItem();
            if (item != Items.AIR) {
                return Optional.of(item);
            }
        }
        ItemStack offhand = player.getOffhandItem();
        return offhand.isEmpty() ? Optional.empty() : Optional.of(offhand.getItem());
    }
}
