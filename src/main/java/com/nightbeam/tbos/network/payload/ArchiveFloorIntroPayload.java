package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Starts the client-side cinematic for a newly activated Archive floor. */
public record ArchiveFloorIntroPayload(long floorIndex, boolean ominous) implements CustomPacketPayload {
    public static final Type<ArchiveFloorIntroPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_floor_intro"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArchiveFloorIntroPayload> STREAM_CODEC =
            StreamCodec.of(ArchiveFloorIntroPayload::write, ArchiveFloorIntroPayload::read);

    public ArchiveFloorIntroPayload {
        if (floorIndex < 0L) {
            throw new IllegalArgumentException("Archive intro floor must not be negative");
        }
    }

    private static void write(RegistryFriendlyByteBuf buffer, ArchiveFloorIntroPayload payload) {
        buffer.writeVarLong(payload.floorIndex);
        buffer.writeBoolean(payload.ominous);
    }

    private static ArchiveFloorIntroPayload read(RegistryFriendlyByteBuf buffer) {
        return new ArchiveFloorIntroPayload(buffer.readVarLong(), buffer.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
