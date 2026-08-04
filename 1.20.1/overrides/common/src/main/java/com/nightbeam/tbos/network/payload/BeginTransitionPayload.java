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

public record BeginTransitionPayload(
        UUID siteId,
        ResourceLocation definitionId,
        BlockPos origin,
        BlockPos center,
        Rotation rotation,
        TemporalState targetState,
        int progressFlags,
        long startTick,
        int durationTicks,
        long effectSeed) implements TbosPacket {

    public static final ResourceLocation ID = new ResourceLocation(Yesterglass.MOD_ID, "begin_transition");

    public BeginTransitionPayload {
        if (!targetState.isStable()) {
            throw new IllegalArgumentException("Transition target must be stable");
        }
        if (durationTicks < 1 || durationTicks > 200) {
            throw new IllegalArgumentException("Transition duration out of bounds: " + durationTicks);
        }
        if (BuiltInTemporalSites.get(definitionId).isEmpty() || progressFlags < 0) {
            throw new IllegalArgumentException("Invalid authored site transition metadata");
        }
    }

    public static BeginTransitionPayload fromSite(TemporalSite site) {
        return new BeginTransitionPayload(
                site.siteId(),
                site.definitionId(),
                site.origin(),
                BuiltInTemporalSites.require(site.definitionId())
                        .transitionCenter(site.origin(), site.rotation()),
                site.rotation(),
                site.state().targetStableState(),
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
        buffer.writeVarInt(targetState.ordinal());
        buffer.writeVarInt(progressFlags);
        buffer.writeVarLong(startTick);
        buffer.writeVarInt(durationTicks);
        buffer.writeLong(effectSeed);
    }

    public static BeginTransitionPayload read(FriendlyByteBuf buffer) {
        return new BeginTransitionPayload(
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
