package com.nightbeam.tbos.run;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/**
 * Lets party members through the seal of a room their party is already fighting in.
 *
 * <p>A combat room seals as soon as the first member is inside and the doorway is
 * clear, which used to shut out anyone a few steps behind until the room was
 * cleared. The seal is still one-way for everything else: monsters stay in, and
 * nobody inside can leave. A member who walks up to a sealed doorway from the
 * corridor or stairwell side is moved just inside instead.
 *
 * <p>This only moves players. It never changes run state, so the encounter
 * manager records the new arrival (visit and checkpoint) on its next tick exactly
 * as it does for anyone who walked in before the seal closed.
 */
public final class ArchivePartyDoors {
    /** How far, in blocks, a member's box may stand from the seal and still be let in. */
    private static final double REACH = 0.25D;

    private ArchivePartyDoors() {
    }

    /** @return how many members were let in this tick */
    public static int admitPartyMembers(ServerLevel level, ArchiveRun run, int roomIndex) {
        if (!sealedInCombat(run, roomIndex)) {
            return 0;
        }
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        int admitted = 0;
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(member.playerId());
            if (player == null
                    || player.level() != level
                    || !player.isAlive()
                    || player.isSpectator()) {
                continue;
            }
            // A member already inside, or fighting behind another room's seal
            // (a stairwell seal is shared by both rooms), is not let through.
            OptionalInt standingIn = ArchiveRoomPlacer.roomContaining(run, player.blockPosition());
            if (standingIn.isPresent()
                    && (standingIn.getAsInt() == roomIndex || sealedInCombat(run, standingIn.getAsInt()))) {
                continue;
            }
            AABB box = player.getBoundingBox();
            for (ArchiveConnection connection : room.connections()) {
                if (!opensForParty(run, room, connection)
                        || !approachVolume(run, roomIndex, connection.direction()).intersects(box)) {
                    continue;
                }
                admit(level, run, roomIndex, connection.direction(), player);
                admitted++;
                break;
            }
        }
        return admitted;
    }

    /** True while a room's own encounter holds its doors shut. */
    public static boolean sealedInCombat(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        return room.runtime().doorsLocked()
                && !room.runtime().completed()
                && run.roomEncounterStates().get(roomIndex).started();
    }

    /**
     * A combat seal marks every route of the room as locked, so the route's own
     * flag cannot tell it apart from a gate. Instead, a route leading to a room
     * that is shut without a fight in it (the final-boss room before its quest
     * is done, the reward room before victory) and an undiscovered secret wall
     * never let anyone in; every other route of a sealed room does.
     */
    public static boolean opensForParty(ArchiveRun run, ArchiveRoomNode room, ArchiveConnection connection) {
        ArchiveRoomNode target = run.dungeonGraph().room(connection.targetRoom());
        boolean gated = target.runtime().doorsLocked()
                && !target.runtime().completed()
                && !run.roomEncounterStates().get(target.index()).started();
        if (gated) {
            return false;
        }
        return !connection.hidden() || target.runtime().secretDiscovered() || room.runtime().secretDiscovered();
    }

    /**
     * The space directly outside a sealed doorway. For a wall door that is the
     * corridor block row against the seal; for a stairwell seal it is the step
     * beyond the seal on the far side from this room. The stairs rise toward
     * +X, so the upper room's outside is the lower half of the stair and the
     * lower room's outside is the upper half.
     */
    public static AABB approachVolume(ArchiveRun run, int roomIndex, ArchiveDirection direction) {
        List<BlockPos> seal = ArchiveRoomPlacer.doorPositions(run, roomIndex, direction);
        int dx;
        int dy;
        int dz;
        if (direction.vertical()) {
            boolean lowerRoom = direction == ArchiveDirection.UP;
            dx = lowerRoom ? 1 : -1;
            dy = lowerRoom ? 1 : -1;
            dz = 0;
        } else {
            dx = direction.stepX();
            dy = 0;
            dz = direction.stepZ();
        }
        if (seal.isEmpty()) {
            throw new IllegalStateException("Archive room " + roomIndex + " has no " + direction + " seal");
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos position : seal) {
            minX = Math.min(minX, position.getX() + dx);
            minY = Math.min(minY, position.getY() + dy);
            minZ = Math.min(minZ, position.getZ() + dz);
            maxX = Math.max(maxX, position.getX() + dx);
            maxY = Math.max(maxY, position.getY() + dy);
            maxZ = Math.max(maxZ, position.getZ() + dz);
        }
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D)
                .inflate(REACH, direction.vertical() ? 0.5D : REACH, REACH);
    }

    private static void admit(
            ServerLevel level, ArchiveRun run, int roomIndex, ArchiveDirection direction, ServerPlayer player) {
        double[] destination = justInside(level, run, roomIndex, direction);
        if (destination == null) {
            BlockPos spawn = ArchiveRoomPlacer.roomSpawn(run, roomIndex);
            destination = new double[] {spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D};
        }
        player.teleportTo(destination[0], destination[1], destination[2]);
        BlockPos at = BlockPos.containing(destination[0], destination[1], destination[2]);
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                destination[0],
                destination[1] + 1.0D,
                destination[2],
                12,
                0.30D,
                0.60D,
                0.30D,
                0.02D);
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    /**
     * Feet position one block inside a wall door, centred across its two-wide
     * opening, or {@code null} when that spot is blocked or the door is a
     * stairwell seal; the caller then falls back to the room's entry marker.
     */
    private static double[] justInside(ServerLevel level, ArchiveRun run, int roomIndex, ArchiveDirection direction) {
        if (direction.vertical()) {
            return null;
        }
        int floorY = ArchiveRoomPlacer.roomBounds(run, roomIndex).minY() + 1;
        List<BlockPos> inside = new ArrayList<>();
        for (BlockPos position : ArchiveRoomPlacer.doorPositions(run, roomIndex, direction)) {
            if (position.getY() == floorY) {
                inside.add(position.offset(-direction.stepX(), 0, -direction.stepZ()));
            }
        }
        if (inside.isEmpty()) {
            return null;
        }
        double x = 0.0D;
        double z = 0.0D;
        for (BlockPos feet : inside) {
            if (!passable(level, feet) || !passable(level, feet.above())) {
                return null;
            }
            x += feet.getX() + 0.5D;
            z += feet.getZ() + 0.5D;
        }
        return new double[] {x / inside.size(), floorY, z / inside.size()};
    }

    private static boolean passable(ServerLevel level, BlockPos position) {
        return level.getBlockState(position).getCollisionShape(level, position).isEmpty();
    }
}
