package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Gameplay role assigned to an immutable room template. */
public enum ArchiveRoomCategory {
    STARTING,
    STANDARD_COMBAT,
    ELITE_COMBAT,
    TREASURE,
    ANCIENT_LIBRARY,
    TRAP,
    PUZZLE,
    SANCTUARY,
    LORE,
    MERCHANT,
    VERTICAL_SHAFT,
    SECRET,
    CURSED,
    MINI_BOSS,
    FINAL_BOSS,
    EXIT_REWARD;

    public static final Codec<ArchiveRoomCategory> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown archive room category: " + value);
                }
            },
            category -> category.name().toLowerCase(Locale.ROOT));

    public boolean mandatory() {
        return this == STARTING || this == FINAL_BOSS || this == EXIT_REWARD;
    }

    public boolean combat() {
        return this == STANDARD_COMBAT || this == ELITE_COMBAT || this == CURSED
                || this == MINI_BOSS || this == FINAL_BOSS;
    }
}
