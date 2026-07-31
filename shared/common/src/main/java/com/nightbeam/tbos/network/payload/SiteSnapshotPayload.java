package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.site.TemporalSite;
import com.nightbeam.tbos.site.BuiltInTemporalSites;
import com.nightbeam.tbos.site.TemporalState;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record SiteSnapshotPayload(
        UUID siteId,
        Identifier definitionId,
        BlockPos origin,
        BlockPos center,
        Rotation rotation,
        TemporalState state,
        int progressFlags,
        long transitionStartTick,
        int transitionDurationTicks,
        long effectSeed) implements CustomPacketPayload {

    public static final Type<SiteSnapshotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "site_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SiteSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            SiteSnapshotPayload::write,
            SiteSnapshotPayload::read);

    public SiteSnapshotPayload {
        if (transitionDurationTicks < 0 || transitionDurationTicks > 200) {
            throw new IllegalArgumentException("Snapshot duration out of bounds: " + transitionDurationTicks);
        }
        if (BuiltInTemporalSites.get(definitionId).isEmpty() || progressFlags < 0) {
            throw new IllegalArgumentException("Invalid authored site snapshot metadata");
        }
    }

    public static SiteSnapshotPayload fromSite(TemporalSite site) {
        return new SiteSnapshotPayload(
                site.siteId(),
                site.definitionId(),
                site.origin(),
                BuiltInTemporalSites.require(site.definitionId())
                        .transitionCenter(site.origin(), site.rotation()),
                site.rotation(),
                site.state(),
                site.progressFlags(),
                site.transitionStartTick(),
                site.transitionDurationTicks(),
                site.effectSeed());
    }

    private static void write(RegistryFriendlyByteBuf buffer, SiteSnapshotPayload payload) {
        buffer.writeUUID(payload.siteId);
        buffer.writeIdentifier(payload.definitionId);
        buffer.writeBlockPos(payload.origin);
        buffer.writeBlockPos(payload.center);
        buffer.writeEnum(payload.rotation);
        buffer.writeVarInt(payload.state.ordinal());
        buffer.writeVarInt(payload.progressFlags);
        buffer.writeVarLong(payload.transitionStartTick);
        buffer.writeVarInt(payload.transitionDurationTicks);
        buffer.writeLong(payload.effectSeed);
    }

    private static SiteSnapshotPayload read(RegistryFriendlyByteBuf buffer) {
        return new SiteSnapshotPayload(
                buffer.readUUID(),
                buffer.readIdentifier(),
                buffer.readBlockPos(),
                buffer.readBlockPos(),
                buffer.readEnum(Rotation.class),
                TemporalState.fromNetworkId(buffer.readVarInt()),
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
