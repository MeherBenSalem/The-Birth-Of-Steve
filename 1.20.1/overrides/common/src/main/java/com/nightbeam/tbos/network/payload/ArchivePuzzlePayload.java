package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Bounded state for the top-left Archive puzzle objective card. */
public record ArchivePuzzlePayload(
        UUID runId,
        int roomIndex,
        PuzzleKind kind,
        PuzzleState state,
        int stage,
        int stageTotal,
        int progress,
        int progressTotal,
        int failures,
        List<Integer> glyphs,
        long serverTick) implements TbosPacket {
    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "archive_puzzle");

    public ArchivePuzzlePayload {
        glyphs = List.copyOf(glyphs);
        if (roomIndex < -1 || roomIndex >= 48
                || stage < 0 || stageTotal < 0 || stage > stageTotal
                || progress < 0 || progressTotal < 0 || progress > progressTotal
                || failures < 0 || failures > 255
                || glyphs.size() > 4
                || glyphs.stream().anyMatch(glyph -> glyph < 0 || glyph > 3)
                || (kind == PuzzleKind.NONE && roomIndex != -1)
                || (kind != PuzzleKind.NONE && roomIndex < 0)) {
            throw new IllegalArgumentException("Invalid Archive puzzle network state");
        }
    }

    public static ArchivePuzzlePayload clear(UUID runId, long serverTick) {
        return new ArchivePuzzlePayload(
                runId,
                -1,
                PuzzleKind.NONE,
                PuzzleState.WAITING,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                serverTick);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(runId);
        buffer.writeVarInt(roomIndex + 1);
        buffer.writeVarInt(kind.ordinal());
        buffer.writeVarInt(state.ordinal());
        buffer.writeVarInt(stage);
        buffer.writeVarInt(stageTotal);
        buffer.writeVarInt(progress);
        buffer.writeVarInt(progressTotal);
        buffer.writeVarInt(failures);
        buffer.writeVarInt(glyphs.size());
        glyphs.forEach(buffer::writeVarInt);
        buffer.writeVarLong(serverTick);
    }

    public static ArchivePuzzlePayload read(FriendlyByteBuf buffer) {
        UUID runId = buffer.readUUID();
        int roomIndex = buffer.readVarInt() - 1;
        PuzzleKind kind = enumValue(PuzzleKind.values(), buffer.readVarInt(), "kind");
        PuzzleState state = enumValue(PuzzleState.values(), buffer.readVarInt(), "state");
        int stage = buffer.readVarInt();
        int stageTotal = buffer.readVarInt();
        int progress = buffer.readVarInt();
        int progressTotal = buffer.readVarInt();
        int failures = buffer.readVarInt();
        int glyphCount = buffer.readVarInt();
        if (glyphCount < 0 || glyphCount > 4) {
            throw new IllegalArgumentException("Archive puzzle glyph count is out of bounds");
        }
        ArrayList<Integer> glyphs = new ArrayList<>(glyphCount);
        for (int index = 0; index < glyphCount; index++) {
            glyphs.add(buffer.readVarInt());
        }
        return new ArchivePuzzlePayload(
                runId,
                roomIndex,
                kind,
                state,
                stage,
                stageTotal,
                progress,
                progressTotal,
                failures,
                glyphs,
                buffer.readVarLong());
    }

    private static <T> T enumValue(T[] values, int ordinal, String field) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Archive puzzle " + field + " is out of bounds");
        }
        return values[ordinal];
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public enum PuzzleKind {
        NONE,
        HALL,
        CHOIR
    }

    public enum PuzzleState {
        WAITING,
        SOLVING,
        COMBAT,
        COMPLETE
    }
}
