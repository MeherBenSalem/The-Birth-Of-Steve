package com.nightbeam.tbos.run;

import com.nightbeam.tbos.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Pure classification for player edits inside a generated Archive instance. */
public final class ArchiveRunProtection {
    private ArchiveRunProtection() {
    }

    public static Decision classify(ArchiveRun run, BlockPos position, BlockState state) {
        if (!ArchiveInstanceLayout.boundsForSlot(run.instanceSlot()).isInside(position)) {
            return Decision.OUTSIDE;
        }
        if (state.is(ModBlocks.ARCHIVE_CACHE.get())) {
            if (position.equals(ArchiveRoomPlacer.rewardCachePosition(run))) {
                return run.status() == ArchiveRunStatus.RETURNING_VICTORY
                                || run.status() == ArchiveRunStatus.COMPLETED
                        ? Decision.CANTOR_CACHE
                        : Decision.DENY;
            }
            if (run.status() == ArchiveRunStatus.ACTIVE
                    && run.dungeonGraph().rooms().stream()
                            .anyMatch(room -> ArchiveRoomPlacer.chestPositions(run, room.index())
                                    .contains(position))) {
                return Decision.ROOM_CACHE;
            }
        }
        if (ModBlocks.ARCHIVE_CRATES.stream().anyMatch(crate -> state.is(crate.get()))
                && run.status() == ArchiveRunStatus.ACTIVE
                && ArchiveRoomPlacer.roomContaining(run, position).isPresent()) {
            return Decision.CRATE_PROP;
        }
        if (run.status() == ArchiveRunStatus.ACTIVE) {
            var roomIndex = ArchiveRoomPlacer.roomContaining(run, position);
            if (roomIndex.isPresent()) {
                var bounds = ArchiveRoomPlacer.roomBounds(run, roomIndex.getAsInt());
                boolean floor = position.getY() == bounds.minY();
                boolean wall = position.getX() == bounds.minX()
                        || position.getX() == bounds.maxX()
                        || position.getZ() == bounds.minZ()
                        || position.getZ() == bounds.maxZ();
                return floor || wall ? Decision.DENY : Decision.BREAKABLE;
            }
        }
        return Decision.DENY;
    }

    public enum Decision {
        OUTSIDE,
        DENY,
        ROOM_CACHE,
        CANTOR_CACHE,
        CRATE_PROP,
        BREAKABLE
    }
}
