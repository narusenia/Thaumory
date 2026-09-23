package one.nxeu.thaumory.jar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.aspect.AspectCodecs;

/** What a jar holds, and the aspect its label names if it has one. Kept on the item and the placed block alike. */
public record JarContents(AspectList aspects, Optional<Aspect> label) {
    public static final JarContents EMPTY = new JarContents(AspectList.empty(), Optional.empty());

    public boolean isEmpty() {
        return aspects.isEmpty() && label.isEmpty();
    }

    public JarContents withAspects(AspectList newAspects) {
        return new JarContents(newAspects, label);
    }

    /** A label can go on a jar holding exactly one aspect, and names that aspect. */
    public Optional<JarContents> labeled() {
        if (label.isPresent() || aspects.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(new JarContents(aspects, Optional.of(aspects.stacks().getFirst().aspect())));
    }

    public JarContents unlabeled() {
        return new JarContents(aspects, Optional.empty());
    }

    public static Codec<JarContents> codec(AspectRegistry registry) {
        // An unknown label (its addon was removed) is dropped rather than failing the whole jar.
        Codec<Aspect> aspect = Identifier.CODEC.comapFlatMap(
                id -> registry.get(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown aspect: " + id)),
                Aspect::id);
        return RecordCodecBuilder.create(i -> i.group(
                AspectCodecs.aspectList(registry).optionalFieldOf("aspects", AspectList.empty()).forGetter(JarContents::aspects),
                aspect.lenientOptionalFieldOf("label").forGetter(JarContents::label)
        ).apply(i, JarContents::new));
    }

    public static StreamCodec<ByteBuf, JarContents> streamCodec(AspectRegistry registry) {
        return ByteBufCodecs.fromCodec(codec(registry));
    }
}
