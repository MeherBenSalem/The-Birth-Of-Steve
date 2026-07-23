package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.run.ArchiveQuestProgress;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Compact, derived Cantor Seal progress sent to active run members. */
public record ArchiveQuestPayload(
        UUID runId,
        int roomsCleared,
        int roomsRequired,
        int lesserBossesDefeated,
        int lesserBossesTotal,
        boolean complete,
        long serverTick) implements CustomPacketPayload {
    public static final Type<ArchiveQuestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_quest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveQuestPayload> STREAM_CODEC =
            StreamCodec.of(ArchiveQuestPayload::write, ArchiveQuestPayload::read);

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

    private static void write(RegistryFriendlyByteBuf buffer, ArchiveQuestPayload payload) {
        buffer.writeUUID(payload.runId);
        buffer.writeVarInt(payload.roomsCleared);
        buffer.writeVarInt(payload.roomsRequired);
        buffer.writeVarInt(payload.lesserBossesDefeated);
        buffer.writeVarInt(payload.lesserBossesTotal);
        buffer.writeBoolean(payload.complete);
        buffer.writeVarLong(payload.serverTick);
    }

    private static ArchiveQuestPayload read(RegistryFriendlyByteBuf buffer) {
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
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
