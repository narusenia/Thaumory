package one.nxeu.thaumory.wand;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.wand.FocusContext;
import one.nxeu.thaumory.api.wand.FocusSpell;
import one.nxeu.thaumory.infusion.InfusionRuntime;
import one.nxeu.thaumory.infusion.ItemEssentia;
import one.nxeu.thaumory.item.ThaumoryComponents;

/**
 * Casting from a wand's focus and filling the wand for it (requirements §17.7). The wand keeps its
 * Essentia in {@code thaumory:stored_essentia}, like infused items.
 */
public final class WandCasting {
    /** A core no datapack describes: as strong as a wooden one. */
    private static final WandPart.Core PLAIN_CORE = new WandPart.Core(1.0, BlockTags.INCORRECT_FOR_STONE_TOOL.location());

    private WandCasting() {}

    public enum Result {
        CAST,
        /** The wand carries no focus the datapacks know. */
        NO_FOCUS,
        NO_ESSENTIA,
        /** The spell found nothing to act on; nothing was paid. */
        NOTHING
    }

    /** The item of the focus on {@code wand}, if it carries one. */
    public static Optional<Identifier> focusItem(ItemStack wand) {
        return Optional.ofNullable(wand.get(ThaumoryComponents.WAND_FOCUS.get()));
    }

    /** The focus on {@code wand}, going by the foci loaded now. */
    public static Optional<WandFocus> focus(ItemStack wand) {
        return focusItem(wand).map(item -> WandFoci.snapshot().get(item));
    }

    /** How much of each aspect the wand's caps hold. */
    public static int capacity(ItemStack wand) {
        WandBuild build = wand.getOrDefault(ThaumoryComponents.WAND_BUILD.get(), WandBuild.DEFAULT);
        return WandParts.cap(WandParts.snapshot(), build.cap()).map(WandPart.Cap::essentia).orElse(0);
    }

    public static WandPart.Core core(ItemStack wand) {
        WandBuild build = wand.getOrDefault(ThaumoryComponents.WAND_BUILD.get(), WandBuild.DEFAULT);
        return WandParts.core(WandParts.snapshot(), build.core()).orElse(PLAIN_CORE);
    }

    public static AspectList stored(ItemStack wand) {
        return wand.getOrDefault(ThaumoryComponents.STORED_ESSENTIA.get(), AspectList.empty());
    }

    /**
     * Takes from {@code offered} what the wand's focus uses, as far as its caps hold; the wand keeps
     * it. Nothing goes in without a focus.
     *
     * @return what was taken
     */
    public static AspectList fill(ItemStack wand, AspectList offered) {
        Optional<WandFocus> focus = focus(wand);
        if (focus.isEmpty()) {
            return AspectList.empty();
        }
        ItemEssentia.Fill fill = ItemEssentia.fill(stored(wand), offered, focus.get().aspects(), capacity(wand));
        if (!fill.taken().isEmpty()) {
            InfusionRuntime.setStored(wand, fill.stored());
        }
        return fill.taken();
    }

    /** Whether {@code wand} would take any of {@code offered}. */
    public static boolean accepts(ItemStack wand, AspectList offered) {
        return focus(wand).map(focus -> !ItemEssentia.fill(stored(wand), offered, focus.aspects(), capacity(wand)).taken().isEmpty())
                .orElse(false);
    }

    /** Casts the spell of the focus on {@code wand}, paying from the wand, and rests the wand after. */
    public static Result cast(ServerLevel level, Player caster, ItemStack wand) {
        Optional<WandFocus> focus = focus(wand);
        Optional<FocusSpell> spell = focus.flatMap(f -> ThaumoryApi.focusSpells().get(f.effect()));
        if (spell.isEmpty()) {
            return Result.NO_FOCUS;
        }
        Context context = new Context(level, caster, wand, focus.get(), core(wand));
        boolean done = spell.get().cast(context);
        if (context.shortOfEssentia) {
            return Result.NO_ESSENTIA;
        }
        if (!done) {
            return Result.NOTHING;
        }
        if (focus.get().cooldown() > 0) {
            caster.getCooldowns().addCooldown(wand, focus.get().cooldown());
        }
        return Result.CAST;
    }

    private static final class Context implements FocusContext {
        private final ServerLevel level;
        private final Player caster;
        private final ItemStack wand;
        private final WandFocus focus;
        private final WandPart.Core core;
        boolean shortOfEssentia;

        Context(ServerLevel level, Player caster, ItemStack wand, WandFocus focus, WandPart.Core core) {
            this.level = level;
            this.caster = caster;
            this.wand = wand;
            this.focus = focus;
            this.core = core;
        }

        @Override
        public ServerLevel level() {
            return level;
        }

        @Override
        public Player caster() {
            return caster;
        }

        @Override
        public ItemStack wand() {
            return wand;
        }

        @Override
        public double power() {
            return core.power();
        }

        @Override
        public TagKey<Block> incorrectFor() {
            return TagKey.create(Registries.BLOCK, core.incorrectFor());
        }

        @Override
        public double setting(String key, double fallback) {
            return focus.setting(key, fallback);
        }

        @Override
        public boolean pay() {
            Optional<AspectList> left = ItemEssentia.pay(stored(wand), focus.cost(), ThaumoryApi.aspects());
            if (left.isEmpty()) {
                shortOfEssentia = true;
                return false;
            }
            InfusionRuntime.setStored(wand, left.get());
            return true;
        }
    }
}
