package one.nxeu.thaumory.crucible;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * {@code data/thaumory/thaumory/crucible.json}. A higher datapack replaces the whole file.
 *
 * <pre>{@code
 * {
 *   "capacity": 500,
 *   "melt_ratio": { "manual": 1.0, "estimated": 0.75 },
 *   "boil_time": 100,
 *   "melt_interval": 5,
 *   "essentia_per_water_level": 100,
 *   "cancel_interval": 20,
 *   "cancel_amount": 1
 * }
 * }</pre>
 *
 * Times are in ticks. While boiling, every {@code cancel_interval} ticks each pair of opposite
 * aspects loses {@code cancel_amount} from both sides.
 */
public record CrucibleSettings(int capacity, MeltRatio meltRatio, int boilTime, int meltInterval, int essentiaPerWaterLevel,
        int cancelInterval, int cancelAmount) {
    public static final CrucibleSettings DEFAULT = new CrucibleSettings(500, new MeltRatio(1.0, 0.75), 100, 5, 100, 20, 1);

    public static final Codec<CrucibleSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("capacity").forGetter(CrucibleSettings::capacity),
            MeltRatio.CODEC.fieldOf("melt_ratio").forGetter(CrucibleSettings::meltRatio),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("boil_time").forGetter(CrucibleSettings::boilTime),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("melt_interval").forGetter(CrucibleSettings::meltInterval),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("essentia_per_water_level").forGetter(CrucibleSettings::essentiaPerWaterLevel),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("cancel_interval").forGetter(CrucibleSettings::cancelInterval),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("cancel_amount").forGetter(CrucibleSettings::cancelAmount)
    ).apply(i, CrucibleSettings::new));

    /** How much of an item's aspects melting yields: items defined in datapacks, and items estimated from recipes. */
    public record MeltRatio(double manual, double estimated) {
        public static final Codec<MeltRatio> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0, 1).fieldOf("manual").forGetter(MeltRatio::manual),
                Codec.doubleRange(0, 1).fieldOf("estimated").forGetter(MeltRatio::estimated)
        ).apply(i, MeltRatio::new));
    }
}
