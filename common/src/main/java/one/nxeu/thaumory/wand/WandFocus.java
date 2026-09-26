package one.nxeu.thaumory.wand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * {@code data/<ns>/thaumory/wand_focus/*.json}: an item that goes onto a wand and the spell it casts
 * (requirements §17.7). The wand holds only the aspects the cost names.
 *
 * <pre>{@code
 * {"item": "thaumory:light_focus", "effect": "thaumory:light", "cost": {"thaumory:lux": 1}, "cooldown": 10, "settings": {"range": 32}}
 * }</pre>
 *
 * @param cooldown ticks the wand rests after a cast
 */
public record WandFocus(Identifier item, Identifier effect, Map<Identifier, Integer> cost, int cooldown, Map<String, Double> settings) {
    public static final Codec<WandFocus> CODEC = RecordCodecBuilder.<WandFocus>create(i -> i.group(
            Identifier.CODEC.fieldOf("item").forGetter(WandFocus::item),
            Identifier.CODEC.fieldOf("effect").forGetter(WandFocus::effect),
            Codec.unboundedMap(Identifier.CODEC, Codec.intRange(1, Integer.MAX_VALUE)).fieldOf("cost").forGetter(WandFocus::cost),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("cooldown", 0).forGetter(WandFocus::cooldown),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("settings", Map.of()).forGetter(WandFocus::settings)
    ).apply(i, WandFocus::new)).validate(focus -> focus.cost().isEmpty()
            ? DataResult.error(() -> "A wand focus needs a cost: " + focus.item()) : DataResult.success(focus));

    public WandFocus {
        cost = Map.copyOf(cost);
        settings = Map.copyOf(settings);
    }

    /** The aspects a wand with this focus takes in. */
    public Set<Identifier> aspects() {
        return cost.keySet();
    }

    public double setting(String key, double fallback) {
        return settings.getOrDefault(key, fallback);
    }
}
