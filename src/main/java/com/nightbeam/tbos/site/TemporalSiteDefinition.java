package com.nightbeam.tbos.site;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;

public record TemporalSiteDefinition(
        Identifier id,
        int sizeX,
        int sizeZ,
        int minY,
        int maxY,
        BlockPos transitionCenter,
        BlockPos memoryAnchor,
        List<BlockPos> phasePlatforms,
        List<BlockPos> ruinRewardPlatforms,
        List<BlockPos> resonanceLamps,
        List<AlignmentMechanismDefinition> alignmentMechanisms,
        List<ChoirBellDefinition> choirBells,
        List<MeridianRelayDefinition> meridianRelays,
        List<OrreryDefinition> orreries) {

    public TemporalSiteDefinition {
        if (sizeX < 1 || sizeX > 64 || sizeZ < 1 || sizeZ > 64) {
            throw new IllegalArgumentException("Temporal site dimensions must be between 1 and 64 blocks");
        }
        if (minY > maxY) {
            throw new IllegalArgumentException("Temporal site minimum Y exceeds maximum Y");
        }
        transitionCenter = transitionCenter.immutable();
        memoryAnchor = validatePosition("memory anchor", memoryAnchor, sizeX, sizeZ, minY, maxY);
        phasePlatforms = validatePositions("phase platform", phasePlatforms, sizeX, sizeZ, minY, maxY);
        ruinRewardPlatforms = validatePositions(
                "ruin reward platform", ruinRewardPlatforms, sizeX, sizeZ, minY, maxY);
        resonanceLamps = validatePositions("resonance lamp", resonanceLamps, sizeX, sizeZ, minY, maxY);
        alignmentMechanisms = List.copyOf(alignmentMechanisms);
        for (AlignmentMechanismDefinition mechanism : alignmentMechanisms) {
            validatePositions(
                    "alignment mechanism",
                    mechanism.allPositions(),
                    sizeX,
                    sizeZ,
                    minY,
                    maxY);
        }
        choirBells = List.copyOf(choirBells);
        for (ChoirBellDefinition bell : choirBells) {
            validatePositions("choir bell", bell.allPositions(), sizeX, sizeZ, minY, maxY);
        }
        meridianRelays = List.copyOf(meridianRelays);
        for (MeridianRelayDefinition relay : meridianRelays) {
            validatePositions("Meridian relay", relay.allPositions(), sizeX, sizeZ, minY, maxY);
        }
        orreries = List.copyOf(orreries);
        for (OrreryDefinition orrery : orreries) {
            validatePositions("Grand Orrery", orrery.allPositions(), sizeX, sizeZ, minY, maxY);
        }
    }

    public BlockPos worldPosition(BlockPos origin, BlockPos relative, Rotation rotation) {
        return origin.offset(transform(relative, rotation));
    }

    public BlockPos transitionCenter(BlockPos origin, Rotation rotation) {
        return worldPosition(origin, transitionCenter, rotation);
    }

    public boolean contains(BlockPos origin, Rotation rotation, BlockPos worldPosition) {
        int localY = worldPosition.getY() - origin.getY();
        if (localY < minY || localY > maxY) {
            return false;
        }
        BlockPos first = transform(BlockPos.ZERO, rotation);
        BlockPos second = transform(new BlockPos(sizeX - 1, 0, sizeZ - 1), rotation);
        int minX = origin.getX() + Math.min(first.getX(), second.getX());
        int maxX = origin.getX() + Math.max(first.getX(), second.getX());
        int minZ = origin.getZ() + Math.min(first.getZ(), second.getZ());
        int maxZ = origin.getZ() + Math.max(first.getZ(), second.getZ());
        return worldPosition.getX() >= minX && worldPosition.getX() <= maxX
                && worldPosition.getZ() >= minZ && worldPosition.getZ() <= maxZ;
    }

    public BlockPos transform(BlockPos relative, Rotation rotation) {
        int x = relative.getX();
        int z = relative.getZ();
        return switch (rotation) {
            case NONE -> relative;
            case CLOCKWISE_90 -> new BlockPos(sizeZ - 1 - z, relative.getY(), x);
            case CLOCKWISE_180 -> new BlockPos(sizeX - 1 - x, relative.getY(), sizeZ - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, relative.getY(), sizeX - 1 - x);
        };
    }

    private static List<BlockPos> validatePositions(
            String label,
            List<BlockPos> positions,
            int sizeX,
            int sizeZ,
            int minY,
            int maxY) {
        List<BlockPos> immutable = positions.stream().map(BlockPos::immutable).distinct().toList();
        for (BlockPos position : immutable) {
            if (position.getX() < 0 || position.getX() >= sizeX
                    || position.getZ() < 0 || position.getZ() >= sizeZ
                    || position.getY() < minY || position.getY() > maxY) {
                throw new IllegalArgumentException("Out-of-bounds " + label + " marker: " + position);
            }
        }
        return List.copyOf(immutable);
    }

    private static BlockPos validatePosition(
            String label,
            BlockPos position,
            int sizeX,
            int sizeZ,
            int minY,
            int maxY) {
        return validatePositions(label, List.of(position), sizeX, sizeZ, minY, maxY).getFirst();
    }
}
