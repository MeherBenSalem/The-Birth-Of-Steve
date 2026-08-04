package com.nightbeam.tbos.network.payload;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.site.TemporalSite;
import com.nightbeam.tbos.site.BuiltInTemporalSites;
import com.nightbeam.tbos.site.TemporalState;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;

public record SiteSnapshotPayload(
        UUID siteId,
        ResourceLocation definitionId,
        BlockPos origin,
        BlockPos center,
        Rotation rotation,
        TemporalState state,
        int progressFlags,
        long transitionStartTick,
        int transitionDurationTicks,
        long effectSeed) implements TbosPacket {

    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "site_snapshot");

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

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(siteId);
        buffer.writeResourceLocation(definitionId);
        buffer.writeBlockPos(origin);
        buffer.writeBlockPos(center);
        buffer.writeEnum(rotation);
        buffer.writeVarInt(state.ordinal());
        buffer.writeVarInt(progressFlags);
        buffer.writeVarLong(transitionStartTick);
        buffer.writeVarInt(transitionDurationTicks);
        buffer.writeLong(effectSeed);
    }

    public static SiteSnapshotPayload read(FriendlyByteBuf buffer) {
        return new SiteSnapshotPayload(
                buffer.readUUID(),
                buffer.readResourceLocation(),
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
    public ResourceLocation id() {
        return ID;
    }
}
