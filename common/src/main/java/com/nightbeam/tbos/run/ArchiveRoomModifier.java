package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum ArchiveRoomModifier {
    DARKNESS,
    TIME_DISTORTION,
    REDUCED_HEALING,
    REINFORCED_ENEMIES,
    CONTINUOUS_WAVES,
    UNSTABLE_FLOORS,
    ANCIENT_CURSE,
    FASTER_TRAPS,
    REGENERATING_GUARDIANS;

    public static final Codec<ArchiveRoomModifier> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown archive room modifier: " + value);
                }
            },
            modifier -> modifier.name().toLowerCase(Locale.ROOT));
}
