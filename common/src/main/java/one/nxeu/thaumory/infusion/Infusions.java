package one.nxeu.thaumory.infusion;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Every effect burnt into one item, in the order they went in. Each effect appears once. */
public record Infusions(List<Infusion> list) {
    public static final Infusions EMPTY = new Infusions(List.of());

    public static final Codec<Infusions> CODEC = Infusion.CODEC.listOf().xmap(Infusions::new, Infusions::list);
    public static final StreamCodec<ByteBuf, Infusions> STREAM_CODEC =
            Infusion.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Infusions::new, Infusions::list);

    public Infusions {
        list = List.copyOf(list);
    }

    /** The capacity these effects take together. */
    public int used() {
        return list.stream().mapToInt(Infusion::capacity).sum();
    }

    /**
     * Whether {@code next} fits in an item of {@code capacity}. An effect already there is being
     * replaced, so its old share does not count.
     */
    public boolean fits(Infusion next, int capacity) {
        int kept = list.stream().filter(infusion -> !infusion.effect().equals(next.effect())).mapToInt(Infusion::capacity).sum();
        return kept + next.capacity() <= capacity;
    }

    /** With {@code infusion} added; one of the same effect already there is replaced where it stood. */
    public Infusions with(Infusion infusion) {
        List<Infusion> next = new ArrayList<>(list);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).effect().equals(infusion.effect())) {
                next.set(i, infusion);
                return new Infusions(next);
            }
        }
        next.add(infusion);
        return new Infusions(next);
    }
}
