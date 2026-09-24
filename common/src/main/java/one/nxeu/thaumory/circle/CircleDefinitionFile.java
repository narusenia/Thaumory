package one.nxeu.thaumory.circle;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * One file under {@code data/<namespace>/thaumory/circle/}: which effect a pair of runes starts,
 * which slot 3 runes it accepts, what it costs, and numbers for the effect.
 *
 * <pre>{@code
 * {
 *   "effect": "thaumory:purification",
 *   "runes": ["thaumory:ordo", "thaumory:lux"],
 *   "slot3": ["none", "thaumory:ignis", "thaumory:aer"],
 *   "mode": "sustained",
 *   "interval": 200,
 *   "settings": { "flux_per_second": 0.5 },
 *   "infusion_cost": { "runes": 32, "parameter": 4 },
 *   "capacity": 2,
 *   "item_cost": 4,
 *   "item_settings": { "radius": 2 }
 * }
 * }</pre>
 *
 * {@code slot3} lists what slot 3 may hold: {@code "none"} for an empty slot, {@code "any"} for
 * any aspect, or an aspect id. A plain {@code "any"} instead of a list means any aspect but not
 * empty. A triggered effect takes {@code cost}, a sustained one {@code interval}. {@code capacity}
 * is how much of an item's capacity the effect takes when infused. {@code item_cost} is what one use
 * of the effect on an item takes from each of its two runes' aspects (the triggered {@code cost} when
 * left out), and {@code item_settings} holds numbers for the effect on an item.
 */
public record CircleDefinitionFile(Identifier effect, List<Identifier> runes, List<String> slot3,
        CircleMode mode, int cost, int interval, Map<String, Double> settings, InfusionCost infusionCost, int capacity,
        Optional<Integer> itemCost, Map<String, Double> itemSettings) {
    public static final String ANY = "any";
    public static final String NONE = "none";

    private static final Codec<List<String>> SLOT3 = Codec.either(Codec.STRING, Codec.STRING.listOf())
            .xmap(either -> either.map(List::of, list -> list), Either::right);

    public static final Codec<CircleDefinitionFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("effect").forGetter(CircleDefinitionFile::effect),
            Identifier.CODEC.listOf(2, 2).fieldOf("runes").forGetter(CircleDefinitionFile::runes),
            SLOT3.optionalFieldOf("slot3", List.of(NONE)).forGetter(CircleDefinitionFile::slot3),
            CircleMode.CODEC.fieldOf("mode").forGetter(CircleDefinitionFile::mode),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("cost", 1).forGetter(CircleDefinitionFile::cost),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("interval", 200).forGetter(CircleDefinitionFile::interval),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("settings", Map.of()).forGetter(CircleDefinitionFile::settings),
            InfusionCost.CODEC.optionalFieldOf("infusion_cost", InfusionCost.DEFAULT).forGetter(CircleDefinitionFile::infusionCost),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("capacity", 1).forGetter(CircleDefinitionFile::capacity),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("item_cost").forGetter(CircleDefinitionFile::itemCost),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("item_settings", Map.of()).forGetter(CircleDefinitionFile::itemSettings)
    ).apply(i, CircleDefinitionFile::new));
}
