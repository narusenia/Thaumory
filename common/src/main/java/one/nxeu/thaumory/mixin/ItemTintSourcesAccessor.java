package one.nxeu.thaumory.mixin;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets Thaumory add its own tint sources, such as a rune's aspect color. */
@Mixin(ItemTintSources.class)
public interface ItemTintSourcesAccessor {
    @Accessor("ID_MAPPER")
    static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> thaumory$idMapper() {
        throw new AssertionError();
    }
}
