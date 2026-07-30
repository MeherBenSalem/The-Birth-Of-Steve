package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** Durable state that changes while players explore one generated room. */
public record ArchiveRoomRuntimeState(
        boolean completed,
        boolean doorsLocked,
        boolean visited,
        boolean secretDiscovered,
        List<Integer> openedContainers,
        boolean uniqueRewardClaimed) {
    public static final ArchiveRoomRuntimeState UNVISITED =
            new ArchiveRoomRuntimeState(false, false, false, false, List.of(), false);
    public static final Codec<ArchiveRoomRuntimeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("completed", false).forGetter(ArchiveRoomRuntimeState::completed),
            Codec.BOOL.optionalFieldOf("doors_locked", false).forGetter(ArchiveRoomRuntimeState::doorsLocked),
            Codec.BOOL.optionalFieldOf("visited", false).forGetter(ArchiveRoomRuntimeState::visited),
            Codec.BOOL.optionalFieldOf("secret_discovered", false).forGetter(ArchiveRoomRuntimeState::secretDiscovered),
            Codec.INT.listOf().optionalFieldOf("opened_containers", List.of())
                    .forGetter(ArchiveRoomRuntimeState::openedContainers),
            Codec.BOOL.optionalFieldOf("unique_reward_claimed", false)
                    .forGetter(ArchiveRoomRuntimeState::uniqueRewardClaimed)
    ).apply(instance, ArchiveRoomRuntimeState::new));

    public ArchiveRoomRuntimeState {
        openedContainers = openedContainers.stream().distinct().sorted().toList();
        if (openedContainers.stream().anyMatch(index -> index < 0 || index > 63)) {
            throw new IllegalArgumentException("Archive container marker indices must be between 0 and 63");
        }
    }

    public ArchiveRoomRuntimeState visit() {
        return new ArchiveRoomRuntimeState(completed, doorsLocked, true, secretDiscovered, openedContainers, uniqueRewardClaimed);
    }

    public ArchiveRoomRuntimeState withDoorsLocked(boolean value) {
        return new ArchiveRoomRuntimeState(completed, value, visited, secretDiscovered, openedContainers, uniqueRewardClaimed);
    }

    public ArchiveRoomRuntimeState complete() {
        return new ArchiveRoomRuntimeState(true, false, true, secretDiscovered, openedContainers, uniqueRewardClaimed);
    }

    public ArchiveRoomRuntimeState discoverSecret() {
        return new ArchiveRoomRuntimeState(completed, doorsLocked, visited, true, openedContainers, uniqueRewardClaimed);
    }

    public ArchiveRoomRuntimeState openContainer(int marker) {
        if (openedContainers.contains(marker)) {
            return this;
        }
        java.util.ArrayList<Integer> opened = new java.util.ArrayList<>(openedContainers);
        opened.add(marker);
        return new ArchiveRoomRuntimeState(completed, doorsLocked, visited, secretDiscovered, opened, uniqueRewardClaimed);
    }

    public ArchiveRoomRuntimeState claimUniqueReward() {
        return uniqueRewardClaimed
                ? this
                : new ArchiveRoomRuntimeState(completed, doorsLocked, visited, secretDiscovered, openedContainers, true);
    }
}
