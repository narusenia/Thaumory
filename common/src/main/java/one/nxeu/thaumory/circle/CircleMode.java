package one.nxeu.thaumory.circle;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** How a circle spends Essentia: once per activation, or on an interval while it runs. */
public enum CircleMode {
    TRIGGERED,
    SUSTAINED;

    public static final Codec<CircleMode> CODEC = Codec.STRING.comapFlatMap(
            name -> {
                for (CircleMode mode : values()) {
                    if (mode.serializedName().equals(name)) {
                        return com.mojang.serialization.DataResult.success(mode);
                    }
                }
                return com.mojang.serialization.DataResult.error(() -> "Unknown circle mode: " + name);
            },
            CircleMode::serializedName);

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
