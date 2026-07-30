package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** Difficulty snapshot selected when an Archive run is first created. */
public enum ArchiveRunMode {
    NORMAL,
    OMINOUS;

    public static final Codec<ArchiveRunMode> CODEC = Codec.STRING.xmap(
            value -> ArchiveRunMode.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));

    public boolean ominous() {
        return this == OMINOUS;
    }
}
