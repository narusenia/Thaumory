package one.nxeu.thaumory.scan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * {@code data/thaumory/thaumory/scanning.json}: {@code {"reveal_after": 3}}. An aspect's name is
 * revealed once the player has scanned this many different items that contain it.
 */
public record ScanSettings(int revealAfter) {
    public static final ScanSettings DEFAULT = new ScanSettings(3);

    public static final Codec<ScanSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("reveal_after").forGetter(ScanSettings::revealAfter)
    ).apply(i, ScanSettings::new));
}
