package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Starts the client-side cinematic for a newly activated Archive floor. */
public record ArchiveFloorIntroPayload(long floorIndex, boolean ominous) implements TbosPacket {
    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "archive_floor_intro");

    public ArchiveFloorIntroPayload {
        if (floorIndex < 0L) {
            throw new IllegalArgumentException("Archive intro floor must not be negative");
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarLong(floorIndex);
        buffer.writeBoolean(ominous);
    }

    public static ArchiveFloorIntroPayload read(FriendlyByteBuf buffer) {
        return new ArchiveFloorIntroPayload(buffer.readVarLong(), buffer.readBoolean());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
