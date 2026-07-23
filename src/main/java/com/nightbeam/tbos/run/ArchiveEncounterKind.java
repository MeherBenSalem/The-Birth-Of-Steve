package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** Persisted encounter contract for a generated room. */
public enum ArchiveEncounterKind {
    EXPLORATION,
    REWARD,
    TRAP,
    SKIRMISH,
    HUNT,
    GUARDIAN,
    HALL,
    CHOIR,
    BOSS;

    public static final Codec<ArchiveEncounterKind> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(ArchiveEncounterKind.valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown archive encounter kind: " + value);
                }
            },
            kind -> kind.name().toLowerCase(Locale.ROOT));
}
