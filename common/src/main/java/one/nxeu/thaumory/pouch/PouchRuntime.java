package one.nxeu.thaumory.pouch;

import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.block.core.CircleCoreBlockEntity;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.infusion.Infusions;
import one.nxeu.thaumory.item.ThaumoryComponents;
import one.nxeu.thaumory.item.ThaumoryItems;
import one.nxeu.thaumory.jar.JarContents;
import one.nxeu.thaumory.wand.WandCasting;
import one.nxeu.thaumory.wand.WandFocus;

/**
 * Tops up, from the jars in a player's Essentia pouches, the Essentia their equipment, amulets and the
 * wands in their hands keep (requirements §17.6).
 */
public final class PouchRuntime {
    private static volatile PouchSettings settings = PouchSettings.DEFAULT;

    private PouchRuntime() {}

    public static void updateSettings(PouchSettings updated) {
        settings = updated;
    }

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer server && server.level().getGameTime() % settings.interval() == 0) {
                refill(server);
            }
        });
    }

    /** One round of topping up. @return whether anything moved */
    public static boolean refill(ServerPlayer player) {
        List<ItemStack> pouches = pouches(player.getInventory());
        if (pouches.isEmpty()) {
            return false;
        }
        boolean moved = false;
        int itemCapacity = CircleCoreBlockEntity.settings().infusion().itemEssentia();
        for (ItemStack item : InfusionRuntime.storingItems(player)) {
            Set<Identifier> accepts = item.getOrDefault(ThaumoryComponents.INFUSIONS.get(), Infusions.EMPTY).storedAspects();
            moved |= topUp(pouches, item, accepts, itemCapacity);
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack wand = player.getItemInHand(hand);
            Optional<WandFocus> focus = WandCasting.focus(wand);
            if (wand.is(ThaumoryItems.WAND.get()) && focus.isPresent()) {
                moved |= topUp(pouches, wand, focus.get().aspects(), WandCasting.capacity(wand));
            }
        }
        return moved;
    }

    private static List<ItemStack> pouches(Inventory inventory) {
        List<ItemStack> pouches = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ThaumoryItems.ESSENTIA_POUCH.get())) {
                pouches.add(inventory.getItem(i));
            }
        }
        return pouches;
    }

    /** Gives {@code item} what it takes of {@code accepts}, from each pouch in turn until it has its share. */
    private static boolean topUp(List<ItemStack> pouches, ItemStack item, Set<Identifier> accepts, int capacity) {
        Set<Aspect> aspects = new LinkedHashSet<>();
        accepts.forEach(id -> ThaumoryApi.aspects().get(id).ifPresent(aspects::add));
        AspectList stored = item.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
        AspectList wanted = PouchRefill.wanted(stored, aspects, capacity, settings.perTransfer());
        AspectList given = AspectList.empty();
        for (ItemStack pouch : pouches) {
            if (wanted.isEmpty()) {
                break;
            }
            List<ItemStack> jars = EssentiaPouchItem.jars(pouch);
            List<AspectList> contents = jars.stream().map(PouchRuntime::contents).toList();
            PouchRefill.Draw draw = PouchRefill.draw(contents, wanted);
            if (draw.taken().isEmpty()) {
                continue;
            }
            List<ItemStack> updated = new ArrayList<>();
            for (int i = 0; i < jars.size(); i++) {
                updated.add(withAspects(jars.get(i), draw.jars().get(i)));
            }
            EssentiaPouchItem.setJars(pouch, updated);
            given = given.plus(draw.taken());
            wanted = wanted.minus(draw.taken());
        }
        if (given.isEmpty()) {
            return false;
        }
        InfusionRuntime.setStored(item, stored.plus(given));
        return true;
    }

    private static AspectList contents(ItemStack jar) {
        return jar.getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY).aspects();
    }

    /** The jar holding {@code aspects} instead; an emptied jar keeps its label. */
    private static ItemStack withAspects(ItemStack jar, AspectList aspects) {
        if (jar.isEmpty()) {
            return jar;
        }
        ItemStack copy = jar.copy();
        JarContents updated = copy.getOrDefault(ThaumoryComponents.JAR_CONTENTS.get(), JarContents.EMPTY).withAspects(aspects);
        if (updated.isEmpty()) {
            copy.remove(ThaumoryComponents.JAR_CONTENTS.get());
        } else {
            copy.set(ThaumoryComponents.JAR_CONTENTS.get(), updated);
        }
        return copy;
    }
}
