package one.nxeu.thaumory.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import one.nxeu.thaumory.Thaumory;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.item.RuneItem;
import one.nxeu.thaumory.mixin.ItemTintSourcesAccessor;

/** {@code "type": "thaumory:rune_aspect"}: a rune's aspect color, or {@code default} when it has none. */
public record RuneTint(int defaultColor) implements ItemTintSource {
    public static final MapCodec<RuneTint> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("default", 0xFFFFFF).forGetter(RuneTint::defaultColor)
    ).apply(i, RuneTint::new));

    /** Makes the type known to item models. Safe to call more than once (datagen calls it too). */
    public static void register() {
        ItemTintSourcesAccessor.thaumory$idMapper().put(Thaumory.id("rune_aspect"), MAP_CODEC);
    }

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        return ARGB.opaque(RuneItem.aspect(stack).map(Aspect::color).orElse(defaultColor));
    }

    @Override
    public MapCodec<RuneTint> type() {
        return MAP_CODEC;
    }
}
