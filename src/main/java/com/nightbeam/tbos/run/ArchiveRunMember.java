package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record ArchiveRunMember(
        UUID playerId,
        ArchiveReturnPoint returnPoint,
        boolean returned,
        boolean rewardClaimed,
        List<Integer> claimedContainers,
        int currentRoom,
        int checkpointRoom) {
    public static final Codec<ArchiveRunMember> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(ArchiveRunMember::playerId),
            ArchiveReturnPoint.CODEC.fieldOf("return_point").forGetter(ArchiveRunMember::returnPoint),
            Codec.BOOL.optionalFieldOf("returned", false).forGetter(ArchiveRunMember::returned),
            Codec.BOOL.optionalFieldOf("reward_claimed", false).forGetter(ArchiveRunMember::rewardClaimed),
            Codec.INT.listOf().optionalFieldOf("claimed_containers", List.of())
                    .forGetter(ArchiveRunMember::claimedContainers),
            Codec.INT.optionalFieldOf("current_room", 0).forGetter(ArchiveRunMember::currentRoom),
            Codec.INT.optionalFieldOf("checkpoint_room", 0).forGetter(ArchiveRunMember::checkpointRoom)
    ).apply(instance, ArchiveRunMember::new));

    public ArchiveRunMember(UUID playerId, ArchiveReturnPoint returnPoint) {
        this(playerId, returnPoint, false, false, List.of(), 0, 0);
    }

    public ArchiveRunMember(UUID playerId, ArchiveReturnPoint returnPoint, boolean returned) {
        this(playerId, returnPoint, returned, false, List.of(), 0, 0);
    }

    public ArchiveRunMember(
            UUID playerId, ArchiveReturnPoint returnPoint, boolean returned, boolean rewardClaimed) {
        this(playerId, returnPoint, returned, rewardClaimed, List.of(), 0, 0);
    }

    public ArchiveRunMember(
            UUID playerId,
            ArchiveReturnPoint returnPoint,
            boolean returned,
            boolean rewardClaimed,
            List<Integer> claimedContainers) {
        this(playerId, returnPoint, returned, rewardClaimed, claimedContainers, 0, 0);
    }

    public ArchiveRunMember {
        playerId = Objects.requireNonNull(playerId, "playerId");
        returnPoint = Objects.requireNonNull(returnPoint, "returnPoint");
        claimedContainers = claimedContainers.stream().distinct().sorted().toList();
        if (claimedContainers.stream().anyMatch(key -> key < 0 || key >= ArchiveRun.MAX_PARTY_CONTAINER_KEYS)) {
            throw new IllegalArgumentException("Archive member container claim is outside the run key range");
        }
        if (currentRoom < 0 || currentRoom >= 48 || checkpointRoom < 0 || checkpointRoom >= 48) {
            throw new IllegalArgumentException("Archive member room state is outside the supported graph range");
        }
    }

    public ArchiveRunMember markReturned() {
        return returned ? this : new ArchiveRunMember(
                playerId, returnPoint, true, rewardClaimed, claimedContainers, currentRoom, checkpointRoom);
    }

    public ArchiveRunMember claimReward() {
        return rewardClaimed ? this : new ArchiveRunMember(
                playerId, returnPoint, returned, true, claimedContainers, currentRoom, checkpointRoom);
    }

    public boolean hasClaimedContainer(int key) {
        return claimedContainers.contains(key);
    }

    public ArchiveRunMember claimContainer(int key) {
        if (hasClaimedContainer(key)) {
            return this;
        }
        java.util.ArrayList<Integer> claims = new java.util.ArrayList<>(claimedContainers);
        claims.add(key);
        return new ArchiveRunMember(
                playerId, returnPoint, returned, rewardClaimed, claims, currentRoom, checkpointRoom);
    }

    public ArchiveRunMember visitRoom(int roomIndex) {
        return roomIndex == currentRoom && roomIndex == checkpointRoom
                ? this
                : new ArchiveRunMember(
                        playerId, returnPoint, returned, rewardClaimed,
                        claimedContainers, roomIndex, roomIndex);
    }

    public ArchiveRunMember checkpoint(int roomIndex) {
        return roomIndex == checkpointRoom
                ? this
                : new ArchiveRunMember(
                        playerId, returnPoint, returned, rewardClaimed,
                        claimedContainers, currentRoom, roomIndex);
    }

    public ArchiveRunMember resetForRegeneration(int startingRoom) {
        return new ArchiveRunMember(
                playerId, returnPoint, false, false, List.of(), startingRoom, startingRoom);
    }
}
