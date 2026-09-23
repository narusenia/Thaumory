package one.nxeu.thaumory.aspect;

import com.mojang.serialization.Codec;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.aspect.AspectStack;

public final class AspectCodecs {
    private AspectCodecs() {}

    /**
     * An aspect list saved as {@code {"thaumory:herba": 4}}. Aspects the registry does not know
     * (an addon was removed) are dropped when loading.
     */
    public static Codec<AspectList> aspectList(AspectRegistry registry) {
        return Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, Integer.MAX_VALUE)).xmap(
                amounts -> {
                    AspectList.Builder list = AspectList.builder();
                    amounts.forEach((id, amount) -> registry.get(id).ifPresent(aspect -> list.add(aspect, amount)));
                    return list.build();
                },
                list -> {
                    Map<Identifier, Integer> amounts = new LinkedHashMap<>();
                    for (AspectStack stack : list.stacks()) {
                        amounts.put(stack.aspect().id(), stack.amount());
                    }
                    return amounts;
                });
    }
}
