package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record ArchiveRoomPlan(
        Identifier roomId,
        int level,
        int slot,
        long encounterSeed,
        ArchiveEncounterKind encounterKind) {
    public static final Codec<ArchiveRoomPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("room_id").forGetter(ArchiveRoomPlan::roomId),
            Codec.INT.fieldOf("level").forGetter(ArchiveRoomPlan::level),
            Codec.INT.fieldOf("slot").forGetter(ArchiveRoomPlan::slot),
            Codec.LONG.fieldOf("encounter_seed").forGetter(ArchiveRoomPlan::encounterSeed),
            ArchiveEncounterKind.CODEC.optionalFieldOf("encounter_kind", ArchiveEncounterKind.SKIRMISH)
                    .forGetter(ArchiveRoomPlan::encounterKind)
    ).apply(instance, ArchiveRoomPlan::new));

    public ArchiveRoomPlan(Identifier roomId, int level, int slot, long encounterSeed) {
        this(roomId, level, slot, encounterSeed, ArchiveEncounterKind.SKIRMISH);
    }

    public ArchiveRoomPlan {
        roomId = Objects.requireNonNull(roomId, "roomId");
        encounterKind = Objects.requireNonNull(encounterKind, "encounterKind");
        if (level < 0 || level > 2) {
            throw new IllegalArgumentException("Archive room level must be between 0 and 2: " + level);
        }
        if (slot < 0 || slot > 63) {
            throw new IllegalArgumentException("Archive room slot must be between 0 and 63: " + slot);
        }
    }
}
