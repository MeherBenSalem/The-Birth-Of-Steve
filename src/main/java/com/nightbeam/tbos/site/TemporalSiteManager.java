package com.nightbeam.tbos.site;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.block.MeridianRelayBlock;
import com.nightbeam.tbos.block.ResonantBellBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TemporalSiteManager {
    public static final int SITE_SIZE = 16;
    public static final int LENS_RANGE = 24;
    private static final int BLOCK_UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final Set<ResourceKey<Level>> ACTIVE_LEVELS = ConcurrentHashMap.newKeySet();

    private TemporalSiteManager() {
    }

    public static TemporalSiteSavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TemporalSiteSavedData.TYPE);
    }

    public static TemporalSite placePrototype(ServerLevel level, BlockPos origin) {
        return placeParallaxAtrium(level, origin, Rotation.NONE);
    }

    public static TemporalSite placeParallaxAtrium(ServerLevel level, BlockPos origin, Rotation rotation) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.parallaxAtrium();
        BlockPos immutableOrigin = origin.immutable();
        ensureDefinitionChunksLoaded(level, definition, immutableOrigin, rotation);

        for (int x = 0; x < definition.sizeX(); x++) {
            for (int z = 0; z < definition.sizeZ(); z++) {
                BlockPos floor = definition.worldPosition(immutableOrigin, new BlockPos(x, 0, z), rotation);
                level.setBlock(floor.below(), ModBlocks.ARCHIVE_STONE.get().defaultBlockState(), BLOCK_UPDATE_FLAGS);
                boolean border = x == 0 || z == 0 || x == definition.sizeX() - 1 || z == definition.sizeZ() - 1;
                level.setBlock(
                        floor,
                        border
                                ? ModBlocks.ARCHIVE_STONE.get().defaultBlockState()
                                : ModBlocks.MERIDIAN_TILE.get().defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int edge = 0; edge < definition.sizeX(); edge++) {
                Block wallBlock = (edge + y) % 5 == 0 ? ModBlocks.CRACKED_ARCHIVE_STONE.get() : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(edge, y, 0), wallBlock);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(edge, y, definition.sizeZ() - 1), wallBlock);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(0, y, edge), wallBlock);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(definition.sizeX() - 1, y, edge), wallBlock);
            }
        }

        // Safe entrance in the south wall and an elevated remembered-state exit in the north wall.
        setRelative(level, definition, immutableOrigin, rotation, new BlockPos(8, 1, 0), Blocks.AIR);
        setRelative(level, definition, immutableOrigin, rotation, new BlockPos(8, 2, 0), Blocks.AIR);
        for (int x = 7; x <= 8; x++) {
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 4, 15), Blocks.AIR);
        }
        level.setBlock(
                definition.worldPosition(immutableOrigin, definition.memoryAnchor(), rotation),
                ModBlocks.MEMORY_ANCHOR.get().defaultBlockState(),
                BLOCK_UPDATE_FLAGS);

        TemporalSiteSavedData data = data(level);
        TemporalSite site = data.register(definition.id(), immutableOrigin, rotation).stable(TemporalState.RUIN);
        data.replace(site);
        applyPhaseGeometry(level, site);
        broadcastSnapshot(level, site);
        return site;
    }

    public static TemporalSite placeHallOfAlignment(ServerLevel level, BlockPos origin, Rotation rotation) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.hallOfAlignment();
        BlockPos immutableOrigin = origin.immutable();
        ensureDefinitionChunksLoaded(level, definition, immutableOrigin, rotation);

        for (int x = 0; x < definition.sizeX(); x++) {
            for (int z = 0; z < definition.sizeZ(); z++) {
                boolean border = x == 0 || z == 0 || x == definition.sizeX() - 1 || z == definition.sizeZ() - 1;
                boolean chasm = x >= 9 && x <= 14 && z >= 9 && z <= 16;
                BlockPos floor = definition.worldPosition(immutableOrigin, new BlockPos(x, 0, z), rotation);
                if (chasm && !border) {
                    level.setBlock(floor, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(floor.below(), Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                } else {
                    level.setBlock(floor.below(), ModBlocks.ARCHIVE_STONE.get().defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(
                            floor,
                            border
                                    ? ModBlocks.ARCHIVE_STONE.get().defaultBlockState()
                                    : ModBlocks.MERIDIAN_TILE.get().defaultBlockState(),
                            BLOCK_UPDATE_FLAGS);
                }
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < definition.sizeX(); x++) {
                Block wall = (x + y) % 6 == 0 ? ModBlocks.CRACKED_ARCHIVE_STONE.get() : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, 0), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, definition.sizeZ() - 1), wall);
            }
            for (int z = 1; z < definition.sizeZ() - 1; z++) {
                Block wall = (z + y) % 6 == 0 ? ModBlocks.CRACKED_ARCHIVE_STONE.get() : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(0, y, z), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(definition.sizeX() - 1, y, z), wall);
            }
        }

        for (int x = 11; x <= 12; x++) {
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 1, 0), Blocks.AIR);
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 2, 0), Blocks.AIR);
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 1, 17), Blocks.AIR);
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 2, 17), Blocks.AIR);
        }
        level.setBlock(
                definition.worldPosition(immutableOrigin, definition.memoryAnchor(), rotation),
                ModBlocks.MEMORY_ANCHOR.get().defaultBlockState(),
                BLOCK_UPDATE_FLAGS);
        for (AlignmentMechanismDefinition mechanism : definition.alignmentMechanisms()) {
            level.setBlock(
                    definition.worldPosition(immutableOrigin, mechanism.target(), rotation),
                    ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState(),
                    BLOCK_UPDATE_FLAGS);
        }

        TemporalSiteSavedData data = data(level);
        TemporalSite site = data.register(definition.id(), immutableOrigin, rotation)
                .stable(TemporalState.RUIN)
                .withProgressFlags(HallAlignmentPuzzle.initialise(0));
        data.replace(site);
        applyPhaseGeometry(level, site);
        broadcastSnapshot(level, site);
        return site;
    }

    public static TemporalSite placeChoirOfHours(ServerLevel level, BlockPos origin, Rotation rotation) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.choirOfHours();
        BlockPos immutableOrigin = origin.immutable();
        ensureDefinitionChunksLoaded(level, definition, immutableOrigin, rotation);

        for (int x = 0; x < definition.sizeX(); x++) {
            for (int z = 0; z < definition.sizeZ(); z++) {
                boolean border = x == 0 || z == 0 || x == definition.sizeX() - 1 || z == definition.sizeZ() - 1;
                boolean chasm = x >= 7 && x <= 12 && z >= 13 && z <= 16;
                BlockPos floor = definition.worldPosition(immutableOrigin, new BlockPos(x, 0, z), rotation);
                if (chasm && !border) {
                    level.setBlock(floor, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(floor.below(), Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                } else {
                    level.setBlock(floor.below(), ModBlocks.ARCHIVE_STONE.get().defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(
                            floor,
                            border
                                    ? ModBlocks.ARCHIVE_STONE.get().defaultBlockState()
                                    : ModBlocks.MERIDIAN_TILE.get().defaultBlockState(),
                            BLOCK_UPDATE_FLAGS);
                }
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < definition.sizeX(); x++) {
                Block wall = (x + y) % 5 == 0 ? ModBlocks.CRACKED_ARCHIVE_STONE.get() : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, 0), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, definition.sizeZ() - 1), wall);
            }
            for (int z = 1; z < definition.sizeZ() - 1; z++) {
                Block wall = (z + y) % 5 == 0 ? ModBlocks.CRACKED_ARCHIVE_STONE.get() : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(0, y, z), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(definition.sizeX() - 1, y, z), wall);
            }
        }

        for (int x = 9; x <= 10; x++) {
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 1, 0), Blocks.AIR);
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 2, 0), Blocks.AIR);
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 1, 17), Blocks.AIR);
            setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, 2, 17), Blocks.AIR);
        }
        level.setBlock(
                definition.worldPosition(immutableOrigin, definition.memoryAnchor(), rotation),
                ModBlocks.MEMORY_ANCHOR.get().defaultBlockState(),
                BLOCK_UPDATE_FLAGS);

        TemporalSiteSavedData data = data(level);
        TemporalSite site = data.register(definition.id(), immutableOrigin, rotation)
                .stable(TemporalState.RUIN)
                .withProgressFlags(ChoirHoursPuzzle.initialise(0));
        data.replace(site);
        applyPhaseGeometry(level, site);
        broadcastSnapshot(level, site);
        return site;
    }

    public static TemporalSite placeBrokenMeridian(ServerLevel level, BlockPos origin, Rotation rotation) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.brokenMeridian();
        BlockPos immutableOrigin = origin.immutable();
        ensureDefinitionChunksLoaded(level, definition, immutableOrigin, rotation);

        for (int x = 0; x < definition.sizeX(); x++) {
            for (int z = 0; z < definition.sizeZ(); z++) {
                boolean border = x == 0 || z == 0 || x == definition.sizeX() - 1 || z == definition.sizeZ() - 1;
                boolean chasm = !border && (z >= 5 && z <= 8 || z >= 19 && z <= 25);
                BlockPos floor = definition.worldPosition(immutableOrigin, new BlockPos(x, 0, z), rotation);
                if (chasm) {
                    level.setBlock(floor, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(floor.below(), Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(floor.below(2), Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(floor.below(3), Blocks.WATER.defaultBlockState(), BLOCK_UPDATE_FLAGS);
                    level.setBlock(
                            floor.below(4),
                            ModBlocks.ARCHIVE_STONE.get().defaultBlockState(),
                            BLOCK_UPDATE_FLAGS);
                } else {
                    level.setBlock(
                            floor.below(),
                            ModBlocks.ARCHIVE_STONE.get().defaultBlockState(),
                            BLOCK_UPDATE_FLAGS);
                    level.setBlock(
                            floor,
                            border
                                    ? ModBlocks.ARCHIVE_STONE.get().defaultBlockState()
                                    : ModBlocks.MERIDIAN_TILE.get().defaultBlockState(),
                            BLOCK_UPDATE_FLAGS);
                }
            }
        }

        for (int y = 1; y <= 5; y++) {
            for (int x = 0; x < definition.sizeX(); x++) {
                Block wall = (x + y) % 7 == 0
                        ? ModBlocks.CRACKED_ARCHIVE_STONE.get()
                        : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, 0), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, 29), wall);
            }
            for (int z = 1; z < definition.sizeZ() - 1; z++) {
                Block wall = (z + y) % 7 == 0
                        ? ModBlocks.CRACKED_ARCHIVE_STONE.get()
                        : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(0, y, z), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(19, y, z), wall);
            }
        }

        for (int x = 9; x <= 10; x++) {
            for (int y = 1; y <= 2; y++) {
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, 0), Blocks.AIR);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, 29), Blocks.AIR);
            }
        }
        level.setBlock(
                definition.worldPosition(immutableOrigin, definition.memoryAnchor(), rotation),
                ModBlocks.MEMORY_ANCHOR.get().defaultBlockState(),
                BLOCK_UPDATE_FLAGS);

        MeridianRelayDefinition relay = definition.meridianRelays().getFirst();
        for (BlockPos socket : relay.positions()) {
            level.setBlock(
                    definition.worldPosition(immutableOrigin, socket.below(), rotation),
                    ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState(),
                    BLOCK_UPDATE_FLAGS);
        }
        for (List<BlockPos> channel : relay.powerChannels()) {
            for (BlockPos segment : channel) {
                level.setBlock(
                        definition.worldPosition(immutableOrigin, segment, rotation),
                        ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
            }
        }
        placeMeridianEscapeLadder(level, definition, immutableOrigin, rotation, 5);
        placeMeridianEscapeLadder(level, definition, immutableOrigin, rotation, 19);

        TemporalSiteSavedData data = data(level);
        TemporalSite site = data.register(definition.id(), immutableOrigin, rotation)
                .stable(TemporalState.RUIN)
                .withProgressFlags(BrokenMeridianPuzzle.initialise(0));
        data.replace(site);
        applyPhaseGeometry(level, site);
        broadcastSnapshot(level, site);
        return site;
    }

    public static TemporalSite placeGrandOrrery(ServerLevel level, BlockPos origin, Rotation rotation) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.grandOrrery();
        BlockPos immutableOrigin = origin.immutable();
        ensureDefinitionChunksLoaded(level, definition, immutableOrigin, rotation);

        for (int x = 0; x < definition.sizeX(); x++) {
            for (int z = 0; z < definition.sizeZ(); z++) {
                boolean border = x == 0 || z == 0 || x == definition.sizeX() - 1 || z == definition.sizeZ() - 1;
                int dx = x - 16;
                int dz = z - 16;
                int radiusSquared = dx * dx + dz * dz;
                boolean engraved = x == 16 || z == 16 || radiusSquared >= 80 && radiusSquared <= 100;
                BlockPos floor = definition.worldPosition(immutableOrigin, new BlockPos(x, 0, z), rotation);
                level.setBlock(
                        floor.below(),
                        ModBlocks.ARCHIVE_STONE.get().defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
                level.setBlock(
                        floor,
                        border
                                ? ModBlocks.ARCHIVE_STONE.get().defaultBlockState()
                                : engraved
                                        ? ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState()
                                        : ModBlocks.MERIDIAN_TILE.get().defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
            }
        }

        for (int y = 1; y <= 7; y++) {
            for (int edge = 0; edge < definition.sizeX(); edge++) {
                Block wall = (edge + y) % 8 == 0
                        ? ModBlocks.CRACKED_ARCHIVE_STONE.get()
                        : ModBlocks.ARCHIVE_STONE.get();
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(edge, y, 0), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(edge, y, 31), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(0, y, edge), wall);
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(31, y, edge), wall);
            }
        }
        for (int x = 15; x <= 16; x++) {
            for (int y = 1; y <= 3; y++) {
                setRelative(level, definition, immutableOrigin, rotation, new BlockPos(x, y, 0), Blocks.AIR);
            }
        }

        OrreryDefinition orrery = definition.orreries().getFirst();
        level.setBlock(
                definition.worldPosition(immutableOrigin, orrery.archiveCore(), rotation),
                ModBlocks.ARCHIVE_CORE.get().defaultBlockState(),
                BLOCK_UPDATE_FLAGS);
        for (BlockPos anchor : orrery.memoryAnchors()) {
            level.setBlock(
                    definition.worldPosition(immutableOrigin, anchor, rotation),
                    ModBlocks.MEMORY_ANCHOR.get().defaultBlockState(),
                    BLOCK_UPDATE_FLAGS);
        }

        TemporalSiteSavedData data = data(level);
        TemporalSite site = data.register(definition.id(), immutableOrigin, rotation);
        LastCuratorEncounterTracker.stop(level, site, true);
        LastCuratorEncounterTracker.clearRewardEntities(level, site);
        site = site.stable(TemporalState.RUIN).withProgressFlags(0);
        data.replace(site);
        applyPhaseGeometry(level, site);
        broadcastSnapshot(level, site);
        return site;
    }

    public static void handleLensUse(ServerPlayer player) {
        ItemStack lens = heldLens(player);
        if (lens.isEmpty()) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(lens)) {
            player.sendSystemMessage(Component.translatable("message.tbos.cooldown"));
            return;
        }

        ServerLevel level = player.level();
        Optional<TemporalSite> nearest = data(level).findNearest(player.blockPosition(), LENS_RANGE);
        if (nearest.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.tbos.no_site"));
            player.getCooldowns().addCooldown(lens, 6);
            return;
        }

        if (beginTransition(level, nearest.get(), player)) {
            player.getCooldowns().addCooldown(lens, 10);
        }
    }

    public static boolean debugToggle(ServerPlayer player) {
        return data(player.level()).findNearest(player.blockPosition(), 128.0D)
                .map(site -> beginTransition(player.level(), site, player))
                .orElse(false);
    }

    public static boolean rotateAlignmentDial(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(pos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.HALL_OF_ALIGNMENT_ID));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        if (site.isTransitioning() || site.state() != TemporalState.REMEMBERED) {
            player.sendSystemMessage(Component.translatable("message.tbos.alignment.remembered_only"));
            return true;
        }
        if (site.hasProgressFlag(HallAlignmentPuzzle.HALL_ALIGNMENT_COMPLETE)) {
            player.sendSystemMessage(Component.translatable("message.tbos.alignment.already_complete"));
            return true;
        }

        List<BlockPos> dials = alignmentDialPositions(site);
        int mechanismIndex = dials.indexOf(pos);
        if (mechanismIndex < 0) {
            return false;
        }

        int flags = HallAlignmentPuzzle.rotateClockwise(site.progressFlags(), mechanismIndex);
        boolean aligned = HallAlignmentPuzzle.isAligned(flags, mechanismIndex);
        boolean completed = HallAlignmentPuzzle.allAligned(flags);
        if (completed) {
            flags = HallAlignmentPuzzle.markComplete(flags);
        }
        TemporalSite updated = site.withProgressFlags(flags);
        data(level).replace(updated);
        applyPhaseGeometry(level, updated);
        broadcastSnapshot(level, updated);
        level.playSound(
                null,
                pos,
                completed ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                completed ? 1.2F : 0.7F,
                completed ? 1.4F : (aligned ? 1.15F : 0.8F));

        Component feedback = Component.translatable(
                aligned
                        ? "message.tbos.alignment.dial_aligned"
                        : "message.tbos.alignment.dial_rotated",
                mechanismIndex + 1);
        notifyNearby(level, updated, feedback);
        if (completed) {
            notifyNearby(level, updated, Component.translatable("message.tbos.alignment.complete"));
            for (ServerPlayer nearby : level.getPlayers(
                    candidate -> updated.distanceToCenterSqr(candidate.blockPosition()) <= 96.0D * 96.0D)) {
                ModAdvancements.awardHallAlignment(nearby);
            }
        }
        return true;
    }

    public static boolean resetAlignmentPuzzle(ServerPlayer player, BlockPos anchorPos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(anchorPos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.HALL_OF_ALIGNMENT_ID))
                .filter(site -> anchorPosition(site).equals(anchorPos));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        if (site.isTransitioning() || site.state() != TemporalState.REMEMBERED) {
            player.sendSystemMessage(Component.translatable("message.tbos.alignment.reset_remembered_only"));
            return true;
        }
        if (site.hasProgressFlag(HallAlignmentPuzzle.HALL_ALIGNMENT_COMPLETE)) {
            player.sendSystemMessage(Component.translatable("message.tbos.alignment.already_complete"));
            return true;
        }

        TemporalSite reset = site.withProgressFlags(HallAlignmentPuzzle.reset(site.progressFlags()));
        data(level).replace(reset);
        applyPhaseGeometry(level, reset);
        broadcastSnapshot(level, reset);
        level.playSound(null, anchorPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.55F);
        notifyNearby(level, reset, Component.translatable("message.tbos.alignment.reset"));
        return true;
    }

    public static boolean ringChoirBell(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(pos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        int bellIndex = choirBellPositions(site).indexOf(pos);
        if (bellIndex < 0) {
            return false;
        }
        if (site.isTransitioning()) {
            player.sendOverlayMessage(Component.translatable("message.tbos.choir.wait"));
            return true;
        }
        if (site.state() == TemporalState.REMEMBERED) {
            player.sendOverlayMessage(Component.translatable("message.tbos.choir.observe"));
            return true;
        }
        if (site.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE)) {
            player.sendOverlayMessage(Component.translatable("message.tbos.choir.already_complete"));
            return true;
        }

        flashChoirBell(level, site, bellIndex, false);
        ChoirHoursPuzzle.Submission submission = ChoirHoursPuzzle.submit(site.progressFlags(), bellIndex);
        TemporalSite updated = site.withProgressFlags(submission.progressFlags());
        data(level).replace(updated);
        if (submission.complete()) {
            applyPhaseGeometry(level, updated);
        }
        broadcastSnapshot(level, updated);

        if (!submission.correct()) {
            notifyNearby(level, updated, Component.translatable(
                    "message.tbos.choir.incorrect",
                    ChoirHoursPuzzle.failedAttempts(updated.progressFlags())));
            if (submission.showStrongHint()) {
                notifyNearby(level, updated, Component.translatable(
                        "message.tbos.choir.strong_hint",
                        symbolComponent(0),
                        symbolComponent(2),
                        symbolComponent(1),
                        symbolComponent(3)));
            }
            return true;
        }

        notifyNearbyOverlay(level, updated, Component.translatable(
                "message.tbos.choir.correct",
                symbolComponent(bellIndex),
                submission.complete() ? ChoirHoursPuzzle.sequence().size() : submission.nextExpectedIndex(),
                ChoirHoursPuzzle.sequence().size()));
        if (submission.complete()) {
            notifyNearby(level, updated, Component.translatable("message.tbos.choir.complete"));
            for (ServerPlayer nearby : level.getPlayers(
                    candidate -> updated.distanceToCenterSqr(candidate.blockPosition()) <= 96.0D * 96.0D)) {
                ModAdvancements.awardChoirOfHours(nearby);
            }
        }
        return true;
    }

    public static boolean resetChoirPuzzle(ServerPlayer player, BlockPos anchorPos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(anchorPos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID))
                .filter(site -> anchorPosition(site).equals(anchorPos));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        if (site.isTransitioning()) {
            player.sendOverlayMessage(Component.translatable("message.tbos.choir.wait"));
            return true;
        }
        if (site.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE)) {
            player.sendOverlayMessage(Component.translatable("message.tbos.choir.already_complete"));
            return true;
        }

        TemporalSite reset = site.withProgressFlags(ChoirHoursPuzzle.resetAttempt(site.progressFlags()));
        data(level).replace(reset);
        broadcastSnapshot(level, reset);
        if (reset.state() == TemporalState.REMEMBERED) {
            ChoirPlaybackTracker.restart(level, reset);
        } else {
            ChoirPlaybackTracker.stop(level, reset, true);
        }
        level.playSound(null, anchorPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.6F);
        notifyNearby(level, reset, Component.translatable("message.tbos.choir.reset"));
        return true;
    }

    public static boolean moveMeridianRelay(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(pos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.BROKEN_MERIDIAN_ID));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        if (site.isTransitioning()) {
            player.sendOverlayMessage(Component.translatable("message.tbos.meridian.wait"));
            return true;
        }
        if (site.state() != TemporalState.REMEMBERED) {
            player.sendOverlayMessage(Component.translatable("message.tbos.meridian.remembered_only"));
            return true;
        }
        if (BrokenMeridianPuzzle.isComplete(site.progressFlags())) {
            player.sendOverlayMessage(Component.translatable("message.tbos.meridian.already_complete"));
            return true;
        }

        List<BlockPos> positions = meridianRelayPositions(site);
        int currentPosition = BrokenMeridianPuzzle.position(site.progressFlags());
        if (currentPosition >= positions.size() || !positions.get(currentPosition).equals(pos)) {
            return false;
        }
        BrokenMeridianPuzzle.Move move = BrokenMeridianPuzzle.advance(site.progressFlags());
        BlockPos destination = positions.get(move.position());
        AABB destinationVolume = new AABB(
                destination.getX() + 0.05D,
                destination.getY(),
                destination.getZ() + 0.05D,
                destination.getX() + 0.95D,
                destination.getY() + 2.0D,
                destination.getZ() + 0.95D);
        if (!level.getEntities(EntityTypeTest.forClass(Entity.class), destinationVolume, Entity::isAlive).isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.tbos.meridian.destination_blocked"));
            return true;
        }

        TemporalSite updated = site.withProgressFlags(move.progressFlags());
        data(level).replace(updated);
        applyPhaseGeometry(level, updated);
        broadcastSnapshot(level, updated);
        level.playSound(
                null,
                destination,
                move.complete() ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                move.complete() ? 1.2F : 0.8F,
                move.complete() ? 1.25F : 0.7F + 0.15F * move.position());
        level.sendParticles(
                ParticleTypes.END_ROD,
                destination.getX() + 0.5D,
                destination.getY() + 1.1D,
                destination.getZ() + 0.5D,
                6,
                0.25D,
                0.25D,
                0.25D,
                0.02D);
        notifyNearbyOverlay(level, updated, Component.translatable(
                "message.tbos.meridian.moved",
                meridianPositionComponent(move.position())));
        if (move.complete()) {
            notifyNearby(level, updated, Component.translatable("message.tbos.meridian.complete"));
            for (ServerPlayer nearby : level.getPlayers(
                    candidate -> updated.distanceToCenterSqr(candidate.blockPosition()) <= 96.0D * 96.0D)) {
                ModAdvancements.awardBrokenMeridian(nearby);
            }
        }
        return true;
    }

    public static boolean resetBrokenMeridianPuzzle(ServerPlayer player, BlockPos anchorPos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(anchorPos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.BROKEN_MERIDIAN_ID))
                .filter(site -> anchorPosition(site).equals(anchorPos));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        if (site.isTransitioning()) {
            player.sendOverlayMessage(Component.translatable("message.tbos.meridian.wait"));
            return true;
        }
        if (BrokenMeridianPuzzle.isComplete(site.progressFlags())) {
            player.sendOverlayMessage(Component.translatable("message.tbos.meridian.already_complete"));
            return true;
        }

        TemporalSite reset = site.withProgressFlags(BrokenMeridianPuzzle.reset(site.progressFlags()));
        data(level).replace(reset);
        applyPhaseGeometry(level, reset);
        broadcastSnapshot(level, reset);
        level.playSound(null, anchorPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.55F);
        notifyNearby(level, reset, Component.translatable("message.tbos.meridian.reset"));
        return true;
    }

    public static boolean startCuratorEncounter(ServerPlayer player, BlockPos corePos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(corePos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.GRAND_ORRERY_ID))
                .filter(site -> orreryCorePositions(site).contains(corePos));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        if (LastCuratorProgress.isDefeated(site.progressFlags())) {
            player.sendSystemMessage(Component.translatable("message.tbos.curator.already_defeated"));
            return true;
        }
        if (LastCuratorProgress.isStarted(site.progressFlags())) {
            LastCuratorEncounterTracker.startIfAbsent(level, site);
            player.sendSystemMessage(Component.translatable("message.tbos.curator.already_active"));
            return true;
        }
        if (site.isTransitioning()) {
            player.sendSystemMessage(Component.translatable("message.tbos.curator.wait"));
            return true;
        }

        TemporalSite started = site.withProgressFlags(LastCuratorProgress.start(site.progressFlags()));
        data(level).replace(started);
        broadcastSnapshot(level, started);
        notifyNearby(level, started, Component.translatable("message.tbos.curator.awakens"));
        if (started.state() == TemporalState.RUIN) {
            beginTransition(level, started, player);
        }
        TemporalSite authoritative = data(level).find(started.siteId()).orElse(started);
        LastCuratorEncounterTracker.startIfAbsent(level, authoritative);
        return true;
    }

    public static boolean activateCuratorAnchor(ServerPlayer player, BlockPos anchorPos) {
        ServerLevel level = player.level();
        Optional<TemporalSite> found = data(level).findContaining(anchorPos)
                .filter(site -> site.definitionId().equals(BuiltInTemporalSites.GRAND_ORRERY_ID))
                .filter(site -> orreryAnchorPositions(site).contains(anchorPos));
        if (found.isEmpty()) {
            return false;
        }
        TemporalSite site = found.get();
        if (!LastCuratorProgress.isStarted(site.progressFlags())) {
            player.sendSystemMessage(Component.translatable("message.tbos.curator.not_started"));
            return true;
        }
        if (LastCuratorProgress.isDefeated(site.progressFlags())) {
            player.sendSystemMessage(Component.translatable("message.tbos.curator.already_defeated"));
            return true;
        }
        notifyNearbyOverlay(level, site, Component.translatable("message.tbos.curator.anchor_triggered"));
        beginTransition(level, site, player);
        return true;
    }

    static boolean beginAutomaticCuratorTransition(ServerLevel level, TemporalSite site) {
        if (site.isTransitioning()) {
            return false;
        }
        return level.getPlayers(player -> site.contains(player.blockPosition()) && !player.isSpectator())
                .stream()
                .findFirst()
                .map(player -> beginTransition(level, site, player))
                .orElse(false);
    }

    static boolean beginTransition(ServerLevel level, TemporalSite site, ServerPlayer feedbackPlayer) {
        if (site.isTransitioning()) {
            feedbackPlayer.sendSystemMessage(Component.translatable("message.tbos.already_transitioning"));
            return false;
        }
        if (!hasLoadedChunks(level, site)) {
            feedbackPlayer.sendSystemMessage(Component.translatable("message.tbos.unloaded"));
            return false;
        }
        TemporalState target = site.state() == TemporalState.RUIN ? TemporalState.REMEMBERED : TemporalState.RUIN;
        if (!isTransitionSafe(level, site, target)) {
            feedbackPlayer.sendSystemMessage(Component.translatable("message.tbos.blocked"));
            return false;
        }
        if (site.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID)) {
            ChoirPlaybackTracker.stop(level, site, true);
        }

        long startTick = level.getGameTime();
        long seed = site.siteId().getMostSignificantBits() ^ site.siteId().getLeastSignificantBits() ^ startTick;
        TemporalSite transitioning = site.beginTransition(startTick, YesterglassConfig.TRANSITION_TICKS.getAsInt(), seed);
        data(level).replace(transitioning);
        ACTIVE_LEVELS.add(level.dimension());
        broadcastTransition(level, transitioning);
        feedbackPlayer.sendSystemMessage(Component.translatable(
                target == TemporalState.REMEMBERED
                        ? "message.tbos.transition_remembered"
                        : "message.tbos.transition_ruin"));
        return true;
    }

    public static void tick(MinecraftServer server) {
        ChoirPlaybackTracker.tick(server);
        LastCuratorEncounterTracker.tick(server);
        if (ACTIVE_LEVELS.isEmpty()) {
            return;
        }
        for (ResourceKey<Level> dimension : List.copyOf(ACTIVE_LEVELS)) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                ACTIVE_LEVELS.remove(dimension);
                continue;
            }
            tickLevel(level);
        }
    }

    private static void tickLevel(ServerLevel level) {
        TemporalSiteSavedData data = data(level);
        for (TemporalSite site : data.all()) {
            if (!site.isTransitioning()) {
                continue;
            }
            TemporalSite finished = site.finishIfDue(level.getGameTime());
            if (finished == site) {
                continue;
            }
            if (!isTransitionSafe(level, site, site.state().targetStableState())) {
                TemporalSite cancelled = site.cancelTransition();
                data.replace(cancelled);
                broadcastSnapshot(level, cancelled);
                notifyNearby(level, site, Component.translatable("message.tbos.blocked_late"));
                continue;
            }
            if (finished.state() == TemporalState.REMEMBERED
                    && finished.definitionId().equals(BuiltInTemporalSites.PARALLAX_ATRIUM_ID)) {
                finished = finished.withProgressFlag(HallAlignmentPuzzle.FIRST_RECONSTRUCTION_COMPLETE);
            }
            if (hasLoadedChunks(level, finished)) {
                applyPhaseGeometry(level, finished);
            }
            data.replace(finished);
            broadcastSnapshot(level, finished);
            if (finished.state() == TemporalState.REMEMBERED
                    && finished.definitionId().equals(BuiltInTemporalSites.PARALLAX_ATRIUM_ID)) {
                TemporalSite completedSite = finished;
                for (ServerPlayer player : level.getPlayers(
                        player -> completedSite.distanceToCenterSqr(player.blockPosition()) <= 96.0D * 96.0D)) {
                    ModAdvancements.awardFirstReconstruction(player);
                }
            }
            if (finished.state() == TemporalState.REMEMBERED
                    && finished.definitionId().equals(BuiltInTemporalSites.HALL_OF_ALIGNMENT_ID)
                    && !finished.hasProgressFlag(HallAlignmentPuzzle.HALL_ALIGNMENT_COMPLETE)) {
                notifyNearby(level, finished, Component.translatable("message.tbos.alignment.begin"));
            }
            if (finished.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID)) {
                if (finished.state() == TemporalState.REMEMBERED
                        && !finished.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE)) {
                    ChoirPlaybackTracker.restart(level, finished);
                    notifyNearby(level, finished, Component.translatable("message.tbos.choir.begin"));
                } else {
                    ChoirPlaybackTracker.stop(level, finished, true);
                }
            }
            if (finished.state() == TemporalState.REMEMBERED
                    && finished.definitionId().equals(BuiltInTemporalSites.BROKEN_MERIDIAN_ID)
                    && !BrokenMeridianPuzzle.isComplete(finished.progressFlags())) {
                notifyNearby(level, finished, Component.translatable("message.tbos.meridian.begin"));
            }
            if (finished.definitionId().equals(BuiltInTemporalSites.GRAND_ORRERY_ID)
                    && LastCuratorProgress.isStarted(finished.progressFlags())
                    && !LastCuratorProgress.isDefeated(finished.progressFlags())) {
                LastCuratorEncounterTracker.startIfAbsent(level, finished);
            }
        }
        if (!data.hasTransitions()) {
            ACTIVE_LEVELS.remove(level.dimension());
        }
    }

    public static void recover(ServerLevel level) {
        TemporalSiteSavedData data = data(level);
        for (TemporalSite site : data.all()) {
            TemporalSite reconciled = site.finishIfDue(level.getGameTime());
            if (reconciled != site) {
                data.replace(reconciled);
            }
            if (reconciled.isTransitioning()) {
                ACTIVE_LEVELS.add(level.dimension());
            } else if (hasLoadedChunks(level, reconciled)) {
                applyPhaseGeometry(level, reconciled);
                if (reconciled.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID)
                        && reconciled.state() == TemporalState.REMEMBERED
                        && !reconciled.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE)) {
                    ChoirPlaybackTracker.startIfAbsent(level, reconciled);
                }
                if (reconciled.definitionId().equals(BuiltInTemporalSites.GRAND_ORRERY_ID)
                        && LastCuratorProgress.isStarted(reconciled.progressFlags())
                        && !LastCuratorProgress.isDefeated(reconciled.progressFlags())) {
                    LastCuratorEncounterTracker.startIfAbsent(level, reconciled);
                }
            }
        }
    }

    public static void onChunkLoaded(ServerLevel level, ChunkPos chunkPos) {
        for (TemporalSite site : data(level).inChunk(chunkPos)) {
            if (!site.isTransitioning() && hasLoadedChunks(level, site)) {
                applyPhaseGeometry(level, site);
                if (site.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID)
                        && site.state() == TemporalState.REMEMBERED
                        && !site.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE)) {
                    ChoirPlaybackTracker.startIfAbsent(level, site);
                }
                if (site.definitionId().equals(BuiltInTemporalSites.GRAND_ORRERY_ID)
                        && LastCuratorProgress.isStarted(site.progressFlags())
                        && !LastCuratorProgress.isDefeated(site.progressFlags())) {
                    LastCuratorEncounterTracker.startIfAbsent(level, site);
                }
            }
        }
    }

    public static void clearRuntimeState() {
        ACTIVE_LEVELS.clear();
        ChoirPlaybackTracker.clear();
        LastCuratorEncounterTracker.clear();
    }

    public static boolean resetNearest(ServerPlayer player) {
        Optional<TemporalSite> nearest = data(player.level()).findNearest(player.blockPosition(), 128.0D);
        if (nearest.isEmpty()) {
            return false;
        }
        TemporalSite reset = nearest.get().stable(TemporalState.RUIN).withProgressFlags(0);
        if (reset.definitionId().equals(BuiltInTemporalSites.HALL_OF_ALIGNMENT_ID)) {
            reset = reset.withProgressFlags(HallAlignmentPuzzle.initialise(0));
        } else if (reset.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID)) {
            reset = reset.withProgressFlags(ChoirHoursPuzzle.initialise(0));
            ChoirPlaybackTracker.stop(player.level(), reset, true);
        } else if (reset.definitionId().equals(BuiltInTemporalSites.BROKEN_MERIDIAN_ID)) {
            reset = reset.withProgressFlags(BrokenMeridianPuzzle.initialise(0));
        } else if (reset.definitionId().equals(BuiltInTemporalSites.GRAND_ORRERY_ID)) {
            LastCuratorEncounterTracker.stop(player.level(), reset, true);
            LastCuratorEncounterTracker.clearRewardEntities(player.level(), reset);
        }
        applyPhaseGeometry(player.level(), reset);
        data(player.level()).replace(reset);
        broadcastSnapshot(player.level(), reset);
        return true;
    }

    public static Optional<TemporalSite> locateNearest(ServerPlayer player) {
        return data(player.level()).findNearest(player.blockPosition(), 4096.0D);
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        if (!YesterglassConfig.PROTECT_ACTIVE_SITE.getAsBoolean()) {
            return false;
        }
        return data(level).findContaining(pos)
                .map(site -> pos.equals(anchorPosition(site))
                        || phasePositions(site).contains(pos)
                        || ruinRewardPositions(site).contains(pos)
                        || lampPositions(site).contains(pos)
                        || alignmentDialPositions(site).contains(pos)
                        || alignmentBeamPositions(site).contains(pos)
                        || alignmentTargetPositions(site).contains(pos)
                        || choirBellPositions(site).contains(pos)
                        || choirImprintPositions(site).contains(pos)
                        || meridianRelayPositions(site).contains(pos)
                        || meridianPowerChannelPositions(site).contains(pos)
                        || orreryCorePositions(site).contains(pos)
                        || orreryAnchorPositions(site).contains(pos)
                        || orreryRingPositions(site).contains(pos))
                .orElse(false);
    }

    public static void sendNearbySnapshots(ServerPlayer player) {
        TemporalSiteSavedData data = data(player.level());
        for (TemporalSite site : data.all()) {
            if (site.distanceToCenterSqr(player.blockPosition()) <= 96.0D * 96.0D) {
                PacketDistributor.sendToPlayer(player, SiteSnapshotPayload.fromSite(site));
            }
        }
    }

    private static ItemStack heldLens(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(ModItems.YESTERGLASS_LENS.get())) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        return offHand.is(ModItems.YESTERGLASS_LENS.get()) ? offHand : ItemStack.EMPTY;
    }

    private static boolean hasLoadedChunks(ServerLevel level, TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        BlockPos origin = site.origin();
        return level.isLoaded(definition.worldPosition(origin, BlockPos.ZERO, site.rotation()))
                && level.isLoaded(definition.worldPosition(
                        origin, new BlockPos(definition.sizeX() - 1, 0, 0), site.rotation()))
                && level.isLoaded(definition.worldPosition(
                        origin, new BlockPos(0, 0, definition.sizeZ() - 1), site.rotation()))
                && level.isLoaded(definition.worldPosition(
                        origin,
                        new BlockPos(definition.sizeX() - 1, 0, definition.sizeZ() - 1),
                        site.rotation()));
    }

    static boolean isSiteLoaded(ServerLevel level, TemporalSite site) {
        return hasLoadedChunks(level, site);
    }

    private static boolean isTransitionSafe(ServerLevel level, TemporalSite site, TemporalState target) {
        if (!target.isStable()) {
            return false;
        }
        List<BlockPos> dynamicPositions = new ArrayList<>(phasePositions(site));
        dynamicPositions.addAll(ruinRewardPositions(site));
        dynamicPositions.addAll(lampPositions(site));
        dynamicPositions.addAll(alignmentDialPositions(site));
        dynamicPositions.addAll(alignmentBeamPositions(site));
        dynamicPositions.addAll(meridianRelayPositions(site));
        for (BlockPos pos : dynamicPositions) {
            AABB safetyVolume = new AABB(
                    pos.getX() - 0.05D,
                    pos.getY() - 0.05D,
                    pos.getZ() - 0.05D,
                    pos.getX() + 1.05D,
                    pos.getY() + 2.05D,
                    pos.getZ() + 1.05D);
            if (!level.getEntities(
                            EntityTypeTest.forClass(LivingEntity.class),
                            safetyVolume,
                            entity -> entity.isAlive()
                                    && !entity.entityTags().contains(LastCuratorEncounterTracker.CURATOR_TAG))
                    .isEmpty()) {
                return false;
            }
        }
        return true;
    }

    static void applyPhaseGeometry(ServerLevel level, TemporalSite site) {
        boolean remembered = site.state().targetStableState() == TemporalState.REMEMBERED;
        for (BlockPos pos : phasePositions(site)) {
            level.setBlock(
                    pos,
                    remembered ? ModBlocks.PHASE_PLATFORM.get().defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    BLOCK_UPDATE_FLAGS);
        }
        for (BlockPos pos : lampPositions(site)) {
            level.setBlock(
                    pos,
                    remembered ? ModBlocks.RESONANCE_LAMP.get().defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    BLOCK_UPDATE_FLAGS);
        }
        boolean ruinRewardUnlocked = site.definitionId().equals(BuiltInTemporalSites.HALL_OF_ALIGNMENT_ID)
                        && site.hasProgressFlag(HallAlignmentPuzzle.HALL_ALIGNMENT_COMPLETE)
                || site.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID)
                        && site.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE)
                || site.definitionId().equals(BuiltInTemporalSites.BROKEN_MERIDIAN_ID)
                        && BrokenMeridianPuzzle.isComplete(site.progressFlags());
        for (BlockPos pos : ruinRewardPositions(site)) {
            Block rewardBlock = site.definitionId().equals(BuiltInTemporalSites.BROKEN_MERIDIAN_ID)
                    ? ModBlocks.CRACKED_ARCHIVE_STONE.get()
                    : ModBlocks.PHASE_PLATFORM.get();
            level.setBlock(
                    pos,
                    !remembered && ruinRewardUnlocked
                            ? rewardBlock.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(),
                    BLOCK_UPDATE_FLAGS);
        }

        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        for (int index = 0; index < definition.alignmentMechanisms().size(); index++) {
            AlignmentMechanismDefinition mechanism = definition.alignmentMechanisms().get(index);
            BlockPos dialPos = definition.worldPosition(site.origin(), mechanism.position(), site.rotation());
            if (remembered) {
                Direction localDirection = HallAlignmentPuzzle.direction(site.progressFlags(), index);
                Direction worldDirection = rotateDirection(localDirection, site.rotation());
                level.setBlock(
                        dialPos,
                        ModBlocks.ALIGNMENT_DIAL.get().defaultBlockState()
                                .setValue(com.nightbeam.tbos.block.AlignmentDialBlock.FACING, worldDirection),
                        BLOCK_UPDATE_FLAGS);
            } else {
                level.setBlock(dialPos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
            boolean showBeam = remembered && HallAlignmentPuzzle.isAligned(site.progressFlags(), index);
            for (BlockPos relativeBeam : mechanism.beamSegments()) {
                BlockPos beamPos = definition.worldPosition(site.origin(), relativeBeam, site.rotation());
                level.setBlock(
                        beamPos,
                        showBeam ? ModBlocks.YESTERGLASS.get().defaultBlockState() : Blocks.AIR.defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
            }
        }

        for (ChoirBellDefinition bell : definition.choirBells()) {
            BlockPos bellPos = definition.worldPosition(site.origin(), bell.position(), site.rotation());
            level.setBlock(
                    bellPos,
                    ModBlocks.RESONANT_BELL.get().defaultBlockState()
                            .setValue(ResonantBellBlock.SYMBOL, bell.symbol())
                            .setValue(ResonantBellBlock.LIT, false),
                    BLOCK_UPDATE_FLAGS);
        }
        if (!remembered || !ChoirPlaybackTracker.isActive(level.dimension(), site.siteId())) {
            clearChoirImprints(level, site);
        }

        for (MeridianRelayDefinition relay : definition.meridianRelays()) {
            int activePosition = BrokenMeridianPuzzle.position(site.progressFlags());
            for (int index = 0; index < relay.positions().size(); index++) {
                BlockPos relayPos = definition.worldPosition(
                        site.origin(), relay.positions().get(index), site.rotation());
                level.setBlock(
                        relayPos,
                        remembered && index == activePosition
                                ? ModBlocks.MERIDIAN_RELAY.get().defaultBlockState()
                                        .setValue(MeridianRelayBlock.POWERED, index == relay.targetPosition())
                                : Blocks.AIR.defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
                for (BlockPos relativeSegment : relay.powerChannels().get(index)) {
                    BlockPos channelPos = definition.worldPosition(
                            site.origin(), relativeSegment, site.rotation());
                    level.setBlock(
                            channelPos,
                            remembered && index == activePosition
                                    ? ModBlocks.YESTERGLASS.get().defaultBlockState()
                                    : ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState(),
                            BLOCK_UPDATE_FLAGS);
                }
            }
        }

        for (OrreryDefinition orrery : definition.orreries()) {
            for (BlockPos relativeRing : orrery.rememberedRingSegments()) {
                level.setBlock(
                        definition.worldPosition(site.origin(), relativeRing, site.rotation()),
                        remembered
                                ? ModBlocks.MEMORY_IMPRINT.get().defaultBlockState()
                                : Blocks.AIR.defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
            }
        }
    }

    private static void broadcastTransition(ServerLevel level, TemporalSite site) {
        BlockPos center = BuiltInTemporalSites.require(site.definitionId())
                .transitionCenter(site.origin(), site.rotation());
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D,
                96.0D,
                BeginTransitionPayload.fromSite(site));
    }

    static void broadcastSnapshot(ServerLevel level, TemporalSite site) {
        BlockPos center = BuiltInTemporalSites.require(site.definitionId())
                .transitionCenter(site.origin(), site.rotation());
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.getX() + 0.5D,
                center.getY() + 0.5D,
                center.getZ() + 0.5D,
                96.0D,
                SiteSnapshotPayload.fromSite(site));
    }

    static void notifyNearby(ServerLevel level, TemporalSite site, Component message) {
        for (ServerPlayer player : level.getPlayers(player -> site.distanceToCenterSqr(player.blockPosition()) <= 96.0D * 96.0D)) {
            player.sendSystemMessage(message);
        }
    }

    static void notifyNearbyOverlay(ServerLevel level, TemporalSite site, Component message) {
        for (ServerPlayer player : level.getPlayers(
                candidate -> site.distanceToCenterSqr(candidate.blockPosition()) <= 96.0D * 96.0D)) {
            player.sendOverlayMessage(message);
        }
    }

    static void showChoirBeat(ServerLevel level, TemporalSite site, int bellIndex) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        if (bellIndex < 0 || bellIndex >= definition.choirBells().size()) {
            return;
        }
        clearChoirImprints(level, site);
        flashChoirBell(level, site, bellIndex, true);
    }

    private static void flashChoirBell(ServerLevel level, TemporalSite site, int bellIndex, boolean showImprint) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        ChoirBellDefinition bell = definition.choirBells().get(bellIndex);
        BlockPos bellPos = definition.worldPosition(site.origin(), bell.position(), site.rotation());
        level.setBlock(
                bellPos,
                ModBlocks.RESONANT_BELL.get().defaultBlockState()
                        .setValue(ResonantBellBlock.SYMBOL, bell.symbol())
                        .setValue(ResonantBellBlock.LIT, true),
                BLOCK_UPDATE_FLAGS);
        level.scheduleTick(bellPos, ModBlocks.RESONANT_BELL.get(), 8);
        if (showImprint) {
            for (BlockPos relative : bell.imprintPositions()) {
                level.setBlock(
                        definition.worldPosition(site.origin(), relative, site.rotation()),
                        ModBlocks.MEMORY_IMPRINT.get().defaultBlockState(),
                        BLOCK_UPDATE_FLAGS);
            }
        }
        level.playSound(
                null,
                bellPos.getX() + 0.5D,
                bellPos.getY() + 0.5D,
                bellPos.getZ() + 0.5D,
                SoundEvents.NOTE_BLOCK_BELL,
                SoundSource.BLOCKS,
                1.0F,
                bell.pitch());
        level.sendParticles(
                ParticleTypes.END_ROD,
                bellPos.getX() + 0.5D,
                bellPos.getY() + 1.1D,
                bellPos.getZ() + 0.5D,
                4,
                0.15D,
                0.2D,
                0.15D,
                0.01D);
        notifyNearbyOverlay(level, site, Component.translatable(
                "subtitles.tbos.resonant_bell",
                symbolComponent(bell.symbol())));
    }

    static void clearChoirImprints(ServerLevel level, TemporalSite site) {
        for (BlockPos pos : choirImprintPositions(site)) {
            if (level.getBlockState(pos).is(ModBlocks.MEMORY_IMPRINT.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }
        }
    }

    private static Component symbolComponent(int symbol) {
        return Component.translatable(switch (symbol) {
            case 0 -> "symbol.tbos.sun";
            case 1 -> "symbol.tbos.moon";
            case 2 -> "symbol.tbos.crown";
            case 3 -> "symbol.tbos.gate";
            default -> throw new IllegalArgumentException("Unknown choir symbol: " + symbol);
        });
    }

    private static Component meridianPositionComponent(int position) {
        return Component.translatable(switch (position) {
            case 0 -> "position.tbos.meridian.west";
            case 1 -> "position.tbos.meridian.center";
            case 2 -> "position.tbos.meridian.east";
            default -> throw new IllegalArgumentException("Unknown Meridian relay position: " + position);
        });
    }

    public static BlockPos anchorPosition(BlockPos origin) {
        return anchorPosition(origin, Rotation.NONE);
    }

    public static BlockPos anchorPosition(BlockPos origin, Rotation rotation) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.parallaxAtrium();
        return definition.worldPosition(origin, definition.memoryAnchor(), rotation);
    }

    public static BlockPos anchorPosition(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.worldPosition(site.origin(), definition.memoryAnchor(), site.rotation());
    }

    public static List<BlockPos> phasePositions(BlockPos origin) {
        TemporalSite site = TemporalSite.create(
                new java.util.UUID(0L, 0L),
                BuiltInTemporalSites.PARALLAX_ATRIUM_ID,
                origin,
                Rotation.NONE);
        return phasePositions(site);
    }

    public static List<BlockPos> phasePositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.phasePlatforms().stream()
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .toList();
    }

    public static List<BlockPos> lampPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.resonanceLamps().stream()
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .toList();
    }

    public static List<BlockPos> ruinRewardPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.ruinRewardPlatforms().stream()
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .toList();
    }

    public static List<BlockPos> alignmentDialPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.alignmentMechanisms().stream()
                .map(mechanism -> definition.worldPosition(site.origin(), mechanism.position(), site.rotation()))
                .toList();
    }

    public static List<BlockPos> alignmentTargetPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.alignmentMechanisms().stream()
                .map(mechanism -> definition.worldPosition(site.origin(), mechanism.target(), site.rotation()))
                .toList();
    }

    public static List<BlockPos> alignmentBeamPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.alignmentMechanisms().stream()
                .flatMap(mechanism -> mechanism.beamSegments().stream())
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .distinct()
                .toList();
    }

    public static List<BlockPos> choirBellPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.choirBells().stream()
                .map(bell -> definition.worldPosition(site.origin(), bell.position(), site.rotation()))
                .toList();
    }

    public static List<BlockPos> choirImprintPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.choirBells().stream()
                .flatMap(bell -> bell.imprintPositions().stream())
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .distinct()
                .toList();
    }

    public static List<BlockPos> meridianRelayPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.meridianRelays().stream()
                .flatMap(relay -> relay.positions().stream())
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .distinct()
                .toList();
    }

    public static List<BlockPos> meridianPowerChannelPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.meridianRelays().stream()
                .flatMap(relay -> relay.powerChannels().stream())
                .flatMap(List::stream)
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .distinct()
                .toList();
    }

    public static List<BlockPos> orreryCorePositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.orreries().stream()
                .map(orrery -> definition.worldPosition(site.origin(), orrery.archiveCore(), site.rotation()))
                .toList();
    }

    public static List<BlockPos> orreryAnchorPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.orreries().stream()
                .flatMap(orrery -> orrery.memoryAnchors().stream())
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .distinct()
                .toList();
    }

    public static List<BlockPos> orreryRingPositions(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        return definition.orreries().stream()
                .flatMap(orrery -> orrery.rememberedRingSegments().stream())
                .map(position -> definition.worldPosition(site.origin(), position, site.rotation()))
                .distinct()
                .toList();
    }

    private static void ensureDefinitionChunksLoaded(
            ServerLevel level,
            TemporalSiteDefinition definition,
            BlockPos origin,
            Rotation rotation) {
        level.getChunkAt(definition.worldPosition(origin, BlockPos.ZERO, rotation));
        level.getChunkAt(definition.worldPosition(
                origin,
                new BlockPos(definition.sizeX() - 1, 0, definition.sizeZ() - 1),
                rotation));
    }

    private static void placeMeridianEscapeLadder(
            ServerLevel level,
            TemporalSiteDefinition definition,
            BlockPos origin,
            Rotation rotation,
            int chasmStartZ) {
        Direction ladderFacing = rotateDirection(Direction.SOUTH, rotation);
        for (int y = -2; y <= 0; y++) {
            BlockPos backing = definition.worldPosition(origin, new BlockPos(2, y, chasmStartZ - 1), rotation);
            BlockPos ladder = definition.worldPosition(origin, new BlockPos(2, y, chasmStartZ), rotation);
            level.setBlock(backing, ModBlocks.ARCHIVE_STONE.get().defaultBlockState(), BLOCK_UPDATE_FLAGS);
            level.setBlock(
                    ladder,
                    Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderFacing),
                    BLOCK_UPDATE_FLAGS);
        }
    }

    private static Direction rotateDirection(Direction direction, Rotation rotation) {
        return switch (rotation) {
            case NONE -> direction;
            case CLOCKWISE_90 -> direction.getClockWise();
            case CLOCKWISE_180 -> direction.getOpposite();
            case COUNTERCLOCKWISE_90 -> direction.getCounterClockWise();
        };
    }

    private static void setRelative(
            ServerLevel level,
            TemporalSiteDefinition definition,
            BlockPos origin,
            Rotation rotation,
            BlockPos relative,
            Block block) {
        BlockPos worldPosition = definition.worldPosition(origin, relative, rotation);
        level.setBlock(worldPosition, block.defaultBlockState(), BLOCK_UPDATE_FLAGS);
    }
}
