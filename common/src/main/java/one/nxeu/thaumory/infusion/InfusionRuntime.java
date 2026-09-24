package one.nxeu.thaumory.infusion;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.infusion.InfusionContext;
import one.nxeu.thaumory.api.infusion.InfusionEffect;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.circle.CircleDefinitions;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.network.UseInfusionPayload;

/**
 * Infused items at work on players (requirements §10.2): passive effects while equipped, the main-hand
 * item's effects when its wearer hits something, and active effects when the key is pressed.
 */
public final class InfusionRuntime {
    /** Where an item counts as equipped. */
    private static final EquipmentSlot[] EQUIPPED = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Where the key looks for an active effect after the main hand, head to feet. */
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public enum UseResult { USED, NONE, NOTHING_TO_ACT_ON, NO_ESSENTIA }

    private record Working(InfusionEffect effect, Context context) {}

    /** The passive effects working for each player as of their last tick, so the ones that stop can be told. */
    private static final Map<UUID, Map<Identifier, Working>> WORKING = new ConcurrentHashMap<>();

    private InfusionRuntime() {}

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server) {
                tick(server);
            }
        });
        PlayerEvent.PLAYER_QUIT.register(InfusionRuntime::stopAll);
        PlayerEvent.ATTACK_ENTITY.register((player, level, target, hand, hit) -> {
            if (level instanceof ServerLevel server && target instanceof LivingEntity living) {
                attack(server, player, living);
            }
            return EventResult.pass();
        });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, UseInfusionPayload.TYPE, UseInfusionPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        use(player);
                    }
                }));
    }

    private static void tick(ServerPlayer player) {
        Map<Identifier, Working> now = passives(player);
        Map<Identifier, Working> before = WORKING.getOrDefault(player.getUUID(), Map.of());
        before.forEach((id, working) -> {
            if (!now.containsKey(id)) {
                working.effect().stop(working.context());
            }
        });
        if (now.isEmpty()) {
            WORKING.remove(player.getUUID());
        } else {
            WORKING.put(player.getUUID(), now);
        }
        long time = player.level().getGameTime();
        now.values().forEach(working -> {
            if (time % Math.max(1, working.effect().period()) == 0) {
                working.effect().tick(working.context());
            }
        });
    }

    /** Each passive effect equipped, at the highest level among the items that carry it. */
    private static Map<Identifier, Working> passives(ServerPlayer player) {
        Map<Identifier, Working> found = new HashMap<>();
        ServerLevel level = player.level();
        for (EquipmentSlot slot : EQUIPPED) {
            ItemStack stack = player.getItemBySlot(slot);
            for (Infusion infusion : equipped(stack).list()) {
                Optional<InfusionEffect> effect = ThaumoryApi.infusionEffects().get(infusion.effect()).filter(e -> !e.active());
                Working current = found.get(infusion.effect());
                if (effect.isPresent() && (current == null || current.context().infusion().level() < infusion.level())) {
                    found.put(infusion.effect(), new Working(effect.get(), new Context(level, player, stack, infusion)));
                }
            }
        }
        return found;
    }

    /** The effects an item carries as equipment. A scroll's effect works only when the scroll is used up. */
    private static Infusions equipped(ItemStack stack) {
        return stack.is(ThaumoryItems.SCROLL.get()) ? Infusions.EMPTY : stack.getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY);
    }

    private static void stopAll(ServerPlayer player) {
        Map<Identifier, Working> working = WORKING.remove(player.getUUID());
        if (working != null) {
            working.values().forEach(w -> w.effect().stop(w.context()));
        }
    }

    private static void attack(ServerLevel level, net.minecraft.world.entity.player.Player player, LivingEntity target) {
        ItemStack stack = player.getMainHandItem();
        for (Infusion infusion : equipped(stack).list()) {
            ThaumoryApi.infusionEffects().get(infusion.effect()).filter(e -> !e.active())
                    .ifPresent(effect -> effect.attack(new Context(level, player, stack, infusion), target));
        }
    }

    /** The key was pressed: uses an active effect and says on the action bar when nothing came of it. */
    private static void use(ServerPlayer player) {
        UseResult result = tryUse(player);
        String key = switch (result) {
            case USED -> null;
            case NONE -> "message.thaumory.infusion_use.none";
            case NOTHING_TO_ACT_ON -> "message.thaumory.infusion_use.nothing";
            case NO_ESSENTIA -> "message.thaumory.infusion_use.no_essentia";
        };
        if (key != null) {
            player.sendOverlayMessage(Component.translatable(key));
            player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }

    /** Uses the active effect of the main-hand item, or else of the first worn piece that has one. */
    public static UseResult tryUse(ServerPlayer player) {
        UseResult result = useIn(player, player.getMainHandItem());
        for (int i = 0; i < ARMOR.length && result == UseResult.NONE; i++) {
            result = useIn(player, player.getItemBySlot(ARMOR[i]));
        }
        return result;
    }

    private static UseResult useIn(ServerPlayer player, ItemStack stack) {
        Optional<Infusion> active = equipped(stack).active();
        Optional<InfusionEffect> effect = active.flatMap(infusion -> ThaumoryApi.infusionEffects().get(infusion.effect()))
                .filter(InfusionEffect::active);
        if (effect.isEmpty()) {
            return UseResult.NONE;
        }
        Context context = new Context(player.level(), player, stack, active.get());
        if (!effect.get().canUse(context)) {
            return UseResult.NOTHING_TO_ACT_ON;
        }
        AspectList stored = stack.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
        Optional<AspectList> left = ItemEssentia.pay(stored, active.get().useCost(), ThaumoryApi.aspects());
        if (left.isEmpty()) {
            return UseResult.NO_ESSENTIA;
        }
        setStored(stack, left.get());
        effect.get().use(context);
        return UseResult.USED;
    }

    /** An infusion at work for {@code wearer}, as effects see it. */
    public static InfusionContext context(ServerLevel level, LivingEntity wearer, ItemStack stack, Infusion infusion) {
        return new Context(level, wearer, stack, infusion);
    }

    /** Puts {@code stored} into the item, removing the component when nothing is left. */
    public static void setStored(ItemStack stack, AspectList stored) {
        if (stored.isEmpty()) {
            stack.remove(ThaumoryComponents.STORED_ESSENTIA.get());
        } else {
            stack.set(ThaumoryComponents.STORED_ESSENTIA.get(), stored);
        }
    }

    /** One infusion at work for one wearer. */
    private record Context(ServerLevel level, LivingEntity wearer, ItemStack stack, Infusion infusion) implements InfusionContext {
        @Override
        public int infusionLevel() {
            return infusion.level();
        }

        @Override
        public Optional<Aspect> parameter() {
            return infusion.parameter().flatMap(ThaumoryApi.aspects()::get);
        }

        @Override
        public double setting(String key, double fallback) {
            return CircleDefinitionReloadListener.definitions().forEffect(infusion.effect())
                    .map(CircleDefinitions.Definition::itemSettings)
                    .map(settings -> settings.getOrDefault(key, fallback))
                    .orElse(fallback);
        }
    }
}
