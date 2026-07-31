package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

public enum ArchiveRunStatus {
    PREPARING,
    ACTIVE,
    RETURNING_VICTORY,
    RETURNING_FAILURE,
    COMPLETED,
    FAILED;

    public static final Codec<ArchiveRunStatus> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(ArchiveRunStatus.valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown archive run status: " + value);
                }
            },
            status -> status.name().toLowerCase(Locale.ROOT));

    public boolean isReturning() {
        return this == RETURNING_VICTORY || this == RETURNING_FAILURE;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean holdsInstanceSlot() {
        return !isTerminal();
    }
}
