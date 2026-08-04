package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.run.ArchiveQuestProgress;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import com.nightbeam.tbos.run.ArchiveRunStatus;
import net.minecraft.network.FriendlyByteBuf;
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
        long serverTick) implements TbosPacket {
    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "journal_quest_snapshot");
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

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(storyMask);
        buffer.writeBoolean(hasRun);
        buffer.writeBoolean(preparing);
        buffer.writeBoolean(ominous);
        buffer.writeVarLong(floorIndex + 1L);
        buffer.writeVarInt(sharedRevives);
        buffer.writeVarInt(roomsCleared);
        buffer.writeVarInt(roomsRequired);
        buffer.writeVarInt(lesserBossesDefeated);
        buffer.writeVarInt(lesserBossesTotal);
        buffer.writeBoolean(gatewayReady);
        buffer.writeVarLong(serverTick);
    }

    public static JournalQuestSnapshotPayload read(FriendlyByteBuf buffer) {
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
    public ResourceLocation id() {
        return ID;
    }
}
