package one.nxeu.thaumory.aspect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
                AspectCodecs::amounts);
    }

    /**
     * Like {@link #aspectList} but fails on aspects the registry does not know, for data where
     * dropping one would silently change its meaning (a recipe getting cheaper).
     */
    public static Codec<AspectList> strictAspectList(AspectRegistry registry) {
        return Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, Integer.MAX_VALUE)).comapFlatMap(
                amounts -> {
                    AspectList.Builder list = AspectList.builder();
                    for (Map.Entry<Identifier, Integer> entry : amounts.entrySet()) {
                        var aspect = registry.get(entry.getKey());
                        if (aspect.isEmpty()) {
                            return DataResult.error(() -> "Unknown aspect: " + entry.getKey());
                        }
                        list.add(aspect.get(), entry.getValue());
                    }
                    return DataResult.success(list.build());
                },
                AspectCodecs::amounts);
    }

    private static Map<Identifier, Integer> amounts(AspectList list) {
        Map<Identifier, Integer> amounts = new LinkedHashMap<>();
        for (AspectStack stack : list.stacks()) {
            amounts.put(stack.aspect().id(), stack.amount());
        }
        return amounts;
    }
}
