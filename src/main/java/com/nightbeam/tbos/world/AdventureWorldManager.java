package com.nightbeam.tbos.world;

import com.nightbeam.tbos.block.FractureCofferBlock;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.site.BuiltInTemporalSites;
import com.nightbeam.tbos.site.TemporalSite;
import com.nightbeam.tbos.site.TemporalSiteManager;
import com.nightbeam.tbos.site.TemporalSiteSavedData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;

public final class AdventureWorldManager {
    public static final int SHRINE_DISCOVERY_RANGE = 14;
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final int[] SHRINE_RADII = {96, 128, 160};

    private AdventureWorldManager() {
    }

    public static List<FractureShrinePlacement> ensureShrines(ServerLevel level, BlockPos near) {
        TemporalSiteSavedData data = TemporalSiteManager.data(level);
        if (!data.fractureShrines().isEmpty()) {
            return data.fractureShrines();
        }

        List<FractureShrinePlacement> placements = new ArrayList<>();
        FractureShrineVariant[] variants = FractureShrineVariant.values();
        int quarterTurn = Math.floorMod((int) (level.getSeed() ^ level.getSeed() >>> 32), 4);
        for (int index = 0; index < variants.length; index++) {
            double angle = (quarterTurn * Math.PI / 2.0D) + index * Math.PI * 2.0D / variants.length;
            int targetX = near.getX() + (int) Math.round(Math.cos(angle) * SHRINE_RADII[index]);
            int targetZ = near.getZ() + (int) Math.round(Math.sin(angle) * SHRINE_RADII[index]);
            BlockPos origin = findDrySurface(level, targetX, targetZ, index * 37);
            placeShrine(level, origin, variants[index]);
            placements.add(new FractureShrinePlacement(variants[index], origin));
        }
        data.setFractureShrines(placements);
        return data.fractureShrines();
    }

    public static BlockPos ensureArchive(ServerLevel level, BlockPos requester) {
        TemporalSiteSavedData data = TemporalSiteManager.data(level);
        if (data.archiveOrigin().isPresent()) {
            return data.archiveOrigin().orElseThrow();
        }

        Optional<TemporalSite> existing = data.all().stream()
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.PARALLAX_ATRIUM_ID))
                .findFirst();
        if (existing.isPresent()) {
            BlockPos origin = existing.get().origin();
            data.setArchiveOrigin(origin);
            return origin;
        }

        List<FractureShrinePlacement> shrines = ensureShrines(level, requester);
        BlockPos center = shrines.stream()
                .min(Comparator.comparingDouble(shrine -> shrine.distanceToSqr(requester)))
                .map(FractureShrinePlacement::origin)
                .orElse(requester);
        int direction = Math.floorMod((int) (level.getSeed() >>> 17), 4);
        int dx = switch (direction) {
            case 0 -> 384;
            case 2 -> -384;
            default -> 0;
        };
        int dz = switch (direction) {
            case 1 -> 384;
            case 3 -> -384;
            default -> 0;
        };
        BlockPos surface = findDrySurface(level, center.getX() + dx, center.getZ() + dz, 113);
        BlockPos origin = new BlockPos((surface.getX() >> 4) << 4, surface.getY() - 1, (surface.getZ() >> 4) << 4);
        placeFullArchive(level, origin);
        data.setArchiveOrigin(origin);
        return origin;
    }

    public static Optional<FractureShrinePlacement> nearestShrine(ServerPlayer player) {
        double rangeSqr = SHRINE_DISCOVERY_RANGE * SHRINE_DISCOVERY_RANGE;
        return TemporalSiteManager.data(player.level()).fractureShrines().stream()
                .filter(shrine -> shrine.distanceToSqr(player.blockPosition()) <= rangeSqr)
                .min(Comparator.comparingDouble(shrine -> shrine.distanceToSqr(player.blockPosition())));
    }

    public static void placeShrine(ServerLevel level, BlockPos origin, FractureShrineVariant variant) {
        origin = clampShrineOrigin(level, origin);
        ensureAreaLoaded(level, origin, 5);
        clearAndFloor(level, origin);
        placeCommonRuins(level, origin);
        switch (variant) {
            case OBSERVATORY -> placeObservatory(level, origin);
            case CURATOR_WORKSHOP -> placeCuratorWorkshop(level, origin);
            case EVACUATION_GATE -> placeEvacuationGate(level, origin);
        }
        level.setBlock(
                origin.offset(0, 0, 2),
                ModBlocks.FRACTURE_COFFER.get().defaultBlockState()
                        .setValue(FractureCofferBlock.VARIANT, variant.ordinal()),
                UPDATE_FLAGS);
    }

    public static void placeFullArchive(ServerLevel level, BlockPos origin) {
        TemporalSiteManager.placeParallaxAtrium(level, origin, Rotation.NONE);
        TemporalSiteManager.placeHallOfAlignment(level, origin.offset(-4, 3, 16), Rotation.NONE);
        TemporalSiteManager.placeChoirOfHours(level, origin.offset(-2, 3, 34), Rotation.NONE);
        TemporalSiteManager.placeBrokenMeridian(level, origin.offset(-2, 3, 52), Rotation.NONE);
        TemporalSiteManager.placeGrandOrrery(level, origin.offset(-8, 3, 82), Rotation.NONE);
    }

    private static BlockPos findDrySurface(ServerLevel level, int targetX, int targetZ, int salt) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = targetX + Math.floorMod(salt * 11 + attempt * 29, 49) - 24;
            int z = targetZ + Math.floorMod(salt * 17 + attempt * 31, 49) - 24;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos surface = clampShrineOrigin(level, new BlockPos(x, y, z));
            if (level.getFluidState(surface.below()).isEmpty()) {
                return surface;
            }
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        return clampShrineOrigin(level, new BlockPos(targetX, y, targetZ));
    }

    private static BlockPos clampShrineOrigin(ServerLevel level, BlockPos origin) {
        // Shrine floors are placed one block below the origin. Empty/minimal-height
        // test worlds can report the dimension floor as their surface, so keep the
        // authored platform inside the buildable range instead of dropping it below
        // the world and leaving the threshold suspended over the void.
        int safeY = Math.max(level.getMinY() + 1, origin.getY());
        return origin.getY() == safeY ? origin : new BlockPos(origin.getX(), safeY, origin.getZ());
    }

    private static void clearAndFloor(ServerLevel level, BlockPos origin) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                Block floor = (Math.abs(x) == 4 || Math.abs(z) == 4 || (x + z & 3) == 0)
                        ? ModBlocks.CRACKED_ARCHIVE_STONE.get()
                        : ModBlocks.ARCHIVE_STONE.get();
                set(level, origin.offset(x, -1, z), floor);
                for (int y = 0; y <= 5; y++) {
                    set(level, origin.offset(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void placeCommonRuins(ServerLevel level, BlockPos origin) {
        for (int x : new int[] {-4, 4}) {
            for (int z : new int[] {-4, 4}) {
                int height = x == z ? 3 : 2;
                for (int y = 0; y < height; y++) {
                    set(level, origin.offset(x, y, z), y == height - 1
                            ? ModBlocks.CRACKED_ARCHIVE_STONE.get()
                            : ModBlocks.ARCHIVE_STONE.get());
                }
            }
        }
        set(level, origin.offset(0, 0, -3), ModBlocks.ENGRAVED_MERIDIAN_TILE.get());
        set(level, origin.offset(0, 0, 0), ModBlocks.RIFT_THRESHOLD.get());
    }

    private static void placeObservatory(ServerLevel level, BlockPos origin) {
        for (int x = -3; x <= 3; x++) {
            int height = Math.abs(x) == 3 ? 1 : Math.abs(x) == 2 ? 3 : 4;
            set(level, origin.offset(x, height, 3), ModBlocks.CHRONICLE_BRONZE.get());
            if (Math.abs(x) >= 2) {
                for (int y = 0; y < height; y++) {
                    set(level, origin.offset(x, y, 3), ModBlocks.CRACKED_ARCHIVE_STONE.get());
                }
            }
        }
        set(level, origin.offset(0, 3, 3), ModBlocks.YESTERGLASS.get());
        set(level, origin.offset(-2, 1, 0), ModBlocks.LENSWORK_CRYSTAL.get());
        set(level, origin.offset(2, 1, 0), ModBlocks.LENSWORK_CRYSTAL.get());
    }

    private static void placeCuratorWorkshop(ServerLevel level, BlockPos origin) {
        for (int y = 0; y <= 3; y++) {
            set(level, origin.offset(-3, y, 3), y == 2 ? ModBlocks.CHRONICLE_BRONZE.get() : ModBlocks.ARCHIVE_STONE.get());
            set(level, origin.offset(3, y, 3), y == 1 ? ModBlocks.CHRONICLE_BRONZE.get() : ModBlocks.ARCHIVE_STONE.get());
        }
        for (int x = -3; x <= 3; x++) {
            set(level, origin.offset(x, 4, 3), x % 2 == 0 ? ModBlocks.CHRONICLE_BRONZE.get() : ModBlocks.CRACKED_ARCHIVE_STONE.get());
        }
        set(level, origin.offset(-2, 0, 0), ModBlocks.MEMORY_ANCHOR.get());
        set(level, origin.offset(2, 0, 0), ModBlocks.ARCHIVE_CORE.get());
        set(level, origin.offset(0, 1, 3), ModBlocks.YESTERGLASS.get());
    }

    private static void placeEvacuationGate(ServerLevel level, BlockPos origin) {
        for (int x : new int[] {-3, 3}) {
            for (int y = 0; y <= 4; y++) {
                set(level, origin.offset(x, y, 3), y == 2
                        ? ModBlocks.CRACKED_ARCHIVE_STONE.get()
                        : ModBlocks.ARCHIVE_STONE.get());
            }
        }
        for (int x = -3; x <= 3; x++) {
            set(level, origin.offset(x, 4, 3), x == 0
                    ? ModBlocks.YESTERGLASS.get()
                    : ModBlocks.CRACKED_ARCHIVE_STONE.get());
        }
        set(level, origin.offset(-1, 0, 0), ModBlocks.CHRONICLE_BRONZE.get());
        set(level, origin.offset(1, 0, 0), ModBlocks.LENSWORK_CRYSTAL.get());
        set(level, origin.offset(0, 0, -1), ModBlocks.ENGRAVED_MERIDIAN_TILE.get());
    }

    private static void ensureAreaLoaded(ServerLevel level, BlockPos origin, int radius) {
        level.getChunkAt(origin.offset(-radius, 0, -radius));
        level.getChunkAt(origin.offset(radius, 0, radius));
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), UPDATE_FLAGS);
    }
}
