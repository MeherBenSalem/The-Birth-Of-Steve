package com.nightbeam.tbos.site;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record TemporalSite(
        UUID siteId,
        Identifier definitionId,
        BlockPos origin,
        Rotation rotation,
        TemporalState state,
        int progressFlags,
        long transitionStartTick,
        int transitionDurationTicks,
        long effectSeed) {

    public static final Codec<TemporalSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("site_id").forGetter(TemporalSite::siteId),
            Identifier.CODEC.optionalFieldOf("definition_id", BuiltInTemporalSites.PARALLAX_ATRIUM_ID)
                    .forGetter(TemporalSite::definitionId),
            BlockPos.CODEC.fieldOf("origin").forGetter(TemporalSite::origin),
            Rotation.CODEC.optionalFieldOf("rotation", Rotation.NONE).forGetter(TemporalSite::rotation),
            TemporalState.CODEC.fieldOf("state").forGetter(TemporalSite::state),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("progress_flags", 0)
                    .forGetter(TemporalSite::progressFlags),
            Codec.LONG.optionalFieldOf("transition_start_tick", 0L).forGetter(TemporalSite::transitionStartTick),
            Codec.intRange(0, 200).optionalFieldOf("transition_duration_ticks", 0).forGetter(TemporalSite::transitionDurationTicks),
            Codec.LONG.optionalFieldOf("effect_seed", 0L).forGetter(TemporalSite::effectSeed)
    ).apply(instance, TemporalSite::new));

    public static TemporalSite ruin(UUID siteId, BlockPos origin) {
        return create(siteId, BuiltInTemporalSites.PARALLAX_ATRIUM_ID, origin, Rotation.NONE);
    }

    public static TemporalSite create(UUID siteId, Identifier definitionId, BlockPos origin, Rotation rotation) {
        return new TemporalSite(siteId, definitionId, origin.immutable(), rotation, TemporalState.RUIN, 0, 0L, 0, 0L);
    }

    public boolean isTransitioning() {
        return !state.isStable();
    }

    public TemporalSite beginTransition(long startTick, int durationTicks, long seed) {
        if (!state.isStable()) {
            return this;
        }
        return new TemporalSite(
                siteId,
                definitionId,
                origin,
                rotation,
                state.transitionToward(),
                progressFlags,
                startTick,
                durationTicks,
                seed);
    }

    public TemporalSite finishIfDue(long gameTick) {
        if (!isTransitioning() || gameTick < transitionStartTick + transitionDurationTicks) {
            return this;
        }
        return stable(state.targetStableState());
    }

    public TemporalSite cancelTransition() {
        return isTransitioning() ? stable(state.previousStableState()) : this;
    }

    public TemporalSite stable(TemporalState stableState) {
        if (!stableState.isStable()) {
            throw new IllegalArgumentException("Expected a stable state, got " + stableState);
        }
        return new TemporalSite(siteId, definitionId, origin, rotation, stableState, progressFlags, 0L, 0, 0L);
    }

    public boolean hasProgressFlag(int flag) {
        return flag > 0 && (progressFlags & flag) == flag;
    }

    public TemporalSite withProgressFlag(int flag) {
        if (flag <= 0) {
            throw new IllegalArgumentException("Progress flags must be positive bit masks");
        }
        return new TemporalSite(
                siteId,
                definitionId,
                origin,
                rotation,
                state,
                progressFlags | flag,
                transitionStartTick,
                transitionDurationTicks,
                effectSeed);
    }

    public TemporalSite withProgressFlags(int flags) {
        if (flags < 0) {
            throw new IllegalArgumentException("Progress flags cannot be negative");
        }
        return new TemporalSite(
                siteId,
                definitionId,
                origin,
                rotation,
                state,
                flags,
                transitionStartTick,
                transitionDurationTicks,
                effectSeed);
    }

    public boolean contains(BlockPos pos) {
        return BuiltInTemporalSites.get(definitionId)
                .map(definition -> definition.contains(origin, rotation, pos))
                .orElse(false);
    }

    public double distanceToCenterSqr(BlockPos pos) {
        BlockPos center = BuiltInTemporalSites.get(definitionId)
                .map(definition -> definition.transitionCenter(origin, rotation))
                .orElse(origin);
        double dx = pos.getX() + 0.5D - (center.getX() + 0.5D);
        double dy = pos.getY() + 0.5D - (center.getY() + 0.5D);
        double dz = pos.getZ() + 0.5D - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }
}
