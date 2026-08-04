package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.run.ArchiveQuestProgress;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Compact, derived Cantor Seal progress sent to active run members. */
public record ArchiveQuestPayload(
        UUID runId,
        int roomsCleared,
        int roomsRequired,
        int lesserBossesDefeated,
        int lesserBossesTotal,
        boolean complete,
        long serverTick) implements TbosPacket {
    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "archive_quest");

    public ArchiveQuestPayload {
        if (roomsCleared < 0 || roomsRequired < 0
                || lesserBossesDefeated < 0 || lesserBossesTotal < 0
                || lesserBossesDefeated > lesserBossesTotal) {
            throw new IllegalArgumentException("Invalid Cantor Seal network progress");
        }
    }

    public static ArchiveQuestPayload from(UUID runId, ArchiveQuestProgress progress, long serverTick) {
        return new ArchiveQuestPayload(
                runId,
                progress.roomsCleared(),
                progress.roomsRequired(),
                progress.lesserBossesDefeated(),
                progress.lesserBossesTotal(),
                progress.complete(),
                serverTick);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(runId);
        buffer.writeVarInt(roomsCleared);
        buffer.writeVarInt(roomsRequired);
        buffer.writeVarInt(lesserBossesDefeated);
        buffer.writeVarInt(lesserBossesTotal);
        buffer.writeBoolean(complete);
        buffer.writeVarLong(serverTick);
    }

    public static ArchiveQuestPayload read(FriendlyByteBuf buffer) {
        return new ArchiveQuestPayload(
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarLong());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
