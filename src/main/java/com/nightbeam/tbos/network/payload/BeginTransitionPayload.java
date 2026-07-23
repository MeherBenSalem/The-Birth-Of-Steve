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

public record BeginTransitionPayload(
        UUID siteId,
        Identifier definitionId,
        BlockPos origin,
        BlockPos center,
        Rotation rotation,
        TemporalState targetState,
        int progressFlags,
        long startTick,
        int durationTicks,
        long effectSeed) implements CustomPacketPayload {

    public static final Type<BeginTransitionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "begin_transition"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeginTransitionPayload> STREAM_CODEC = StreamCodec.of(
            BeginTransitionPayload::write,
            BeginTransitionPayload::read);

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

    private static void write(RegistryFriendlyByteBuf buffer, BeginTransitionPayload payload) {
        buffer.writeUUID(payload.siteId);
        buffer.writeIdentifier(payload.definitionId);
        buffer.writeBlockPos(payload.origin);
        buffer.writeBlockPos(payload.center);
        buffer.writeEnum(payload.rotation);
        buffer.writeVarInt(payload.targetState.ordinal());
        buffer.writeVarInt(payload.progressFlags);
        buffer.writeVarLong(payload.startTick);
        buffer.writeVarInt(payload.durationTicks);
        buffer.writeLong(payload.effectSeed);
    }

    private static BeginTransitionPayload read(RegistryFriendlyByteBuf buffer) {
        return new BeginTransitionPayload(
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
