package com.nightbeam.tbos.site;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum TemporalState {
    RUIN,
    REMEMBERED,
    TRANSITION_TO_RUIN,
    TRANSITION_TO_REMEMBERED;

    public static final Codec<TemporalState> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(TemporalState.valueOf(value));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown temporal state: " + value);
                }
            },
            TemporalState::name);

    public boolean isStable() {
        return this == RUIN || this == REMEMBERED;
    }

    public TemporalState targetStableState() {
        return switch (this) {
            case RUIN, TRANSITION_TO_RUIN -> RUIN;
            case REMEMBERED, TRANSITION_TO_REMEMBERED -> REMEMBERED;
        };
    }

    public TemporalState previousStableState() {
        return switch (this) {
            case RUIN, TRANSITION_TO_REMEMBERED -> RUIN;
            case REMEMBERED, TRANSITION_TO_RUIN -> REMEMBERED;
        };
    }

    public TemporalState transitionToward() {
        return switch (this) {
            case RUIN -> TRANSITION_TO_REMEMBERED;
            case REMEMBERED -> TRANSITION_TO_RUIN;
            default -> this;
        };
    }

    public static TemporalState fromNetworkId(int id) {
        TemporalState[] values = values();
        if (id < 0 || id >= values.length) {
            throw new IllegalArgumentException("Invalid temporal state id: " + id);
        }
        return values[id];
    }
}
