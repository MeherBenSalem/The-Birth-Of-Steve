package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.run.ArchiveQuestProgress;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import com.nightbeam.tbos.run.ArchiveRunStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned story and active-run state displayed by the Archivist's Journal. */
public record JournalQuestSnapshotPayload(
        int storyMask,
        boolean hasRun,
        boolean preparing,
        boolean ominous,
        long floorIndex,
        int sharedRevives,
        int roomsCleared,
        int roomsRequired,
        int lesserBossesDefeated,
        int lesserBossesTotal,
        boolean gatewayReady,
        long serverTick) implements CustomPacketPayload {
    public static final Type<JournalQuestSnapshotPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "journal_quest_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JournalQuestSnapshotPayload> STREAM_CODEC =
            StreamCodec.of(JournalQuestSnapshotPayload::write, JournalQuestSnapshotPayload::read);
    private static final int STORY_MASK_LIMIT = (1 << 10) - 1;

    public JournalQuestSnapshotPayload {
        if (storyMask < 0 || (storyMask & ~STORY_MASK_LIMIT) != 0
                || floorIndex < -1L
                || sharedRevives < 0 || sharedRevives > ArchiveRun.MAX_SHARED_REVIVES
                || roomsCleared < 0 || roomsRequired < 0 || roomsCleared > roomsRequired
                || lesserBossesDefeated < 0 || lesserBossesTotal < 0
                || lesserBossesDefeated > lesserBossesTotal
                || (!hasRun && (preparing || ominous || floorIndex != -1L || gatewayReady))) {
            throw new IllegalArgumentException("Invalid Archivist Journal quest snapshot");
        }
    }

    public static JournalQuestSnapshotPayload from(ServerPlayer player) {
        ArchiveRun run = ArchiveRunSavedData.get(player.level().getServer())
                .findByMember(player.getUUID())
                .orElse(null);
        if (run == null) {
            return new JournalQuestSnapshotPayload(
                    ModAdvancements.journalStoryMask(player),
                    false,
                    false,
                    false,
                    -1L,
                    0,
                    0,
                    0,
                    0,
                    0,
                    false,
                    player.level().getServer().getTickCount());
        }
        ArchiveQuestProgress progress = ArchiveQuestProgress.from(run.dungeonGraph());
        boolean gatewayReady = run.status() == ArchiveRunStatus.ACTIVE
                && run.dungeonGraph().room(run.dungeonGraph().rewardRoom()).runtime().completed();
        return new JournalQuestSnapshotPayload(
                ModAdvancements.journalStoryMask(player),
                true,
                run.status() == ArchiveRunStatus.PREPARING,
                run.mode().ominous(),
                run.floor(),
                run.sharedRevives(),
                Math.min(progress.roomsCleared(), progress.roomsRequired()),
                progress.roomsRequired(),
                progress.lesserBossesDefeated(),
                progress.lesserBossesTotal(),
                gatewayReady,
                player.level().getServer().getTickCount());
    }

    private static void write(RegistryFriendlyByteBuf buffer, JournalQuestSnapshotPayload payload) {
        buffer.writeVarInt(payload.storyMask);
        buffer.writeBoolean(payload.hasRun);
        buffer.writeBoolean(payload.preparing);
        buffer.writeBoolean(payload.ominous);
        buffer.writeVarLong(payload.floorIndex + 1L);
        buffer.writeVarInt(payload.sharedRevives);
        buffer.writeVarInt(payload.roomsCleared);
        buffer.writeVarInt(payload.roomsRequired);
        buffer.writeVarInt(payload.lesserBossesDefeated);
        buffer.writeVarInt(payload.lesserBossesTotal);
        buffer.writeBoolean(payload.gatewayReady);
        buffer.writeVarLong(payload.serverTick);
    }

    private static JournalQuestSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        return new JournalQuestSnapshotPayload(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarLong() - 1L,
                buffer.readVarInt(),
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
