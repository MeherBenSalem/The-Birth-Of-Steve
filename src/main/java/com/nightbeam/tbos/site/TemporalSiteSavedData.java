package com.nightbeam.tbos.site;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.world.FractureShrinePlacement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class TemporalSiteSavedData extends SavedData {
    public static final int SCHEMA_REVISION = 3;

    private static final Codec<TemporalSiteSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_revision", SCHEMA_REVISION).forGetter(data -> SCHEMA_REVISION),
            TemporalSite.CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(data -> List.copyOf(data.sites.values())),
            FractureShrinePlacement.CODEC.listOf().optionalFieldOf("fracture_shrines", List.of())
                    .forGetter(TemporalSiteSavedData::fractureShrines),
            BlockPos.CODEC.optionalFieldOf("archive_origin").forGetter(data -> data.archiveOrigin)
    ).apply(instance, TemporalSiteSavedData::fromCodec));

    public static final SavedDataType<TemporalSiteSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "temporal_sites"),
            TemporalSiteSavedData::new,
            CODEC);

    private final Map<UUID, TemporalSite> sites = new LinkedHashMap<>();
    private final Map<Long, Set<UUID>> sitesByChunk = new LinkedHashMap<>();
    private List<FractureShrinePlacement> fractureShrines = List.of();
    private Optional<BlockPos> archiveOrigin = Optional.empty();

    public TemporalSite register(BlockPos origin) {
        return register(BuiltInTemporalSites.PARALLAX_ATRIUM_ID, origin, Rotation.NONE);
    }

    public TemporalSite register(Identifier definitionId, BlockPos origin, Rotation rotation) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(definitionId);
        Optional<TemporalSite> existing = sites.values().stream().filter(site -> site.origin().equals(origin)).findFirst();
        if (existing.isPresent()) {
            TemporalSite site = existing.get();
            if (!site.definitionId().equals(definitionId) || site.rotation() != rotation) {
                Yesterglass.LOGGER.warn(
                        "Ignored conflicting temporal site registration at {}: existing {} {}, requested {} {}",
                        origin,
                        site.definitionId(),
                        site.rotation(),
                        definitionId,
                        rotation);
            }
            return existing.get();
        }
        UUID id = UUID.nameUUIDFromBytes(
                ("tbos-site:" + definition.id() + ":" + origin.asLong() + ":" + rotation.getSerializedName())
                        .getBytes(StandardCharsets.UTF_8));
        TemporalSite site = TemporalSite.create(id, definitionId, origin, rotation);
        sites.put(id, site);
        index(site);
        setDirty();
        return site;
    }

    public void replace(TemporalSite site) {
        sites.put(site.siteId(), site);
        index(site);
        setDirty();
    }

    public Optional<TemporalSite> find(UUID siteId) {
        return Optional.ofNullable(sites.get(siteId));
    }

    public Optional<TemporalSite> findNearest(BlockPos pos, double maxDistance) {
        double maxDistanceSqr = maxDistance * maxDistance;
        return sites.values().stream()
                .filter(site -> site.distanceToCenterSqr(pos) <= maxDistanceSqr)
                .min((left, right) -> Double.compare(left.distanceToCenterSqr(pos), right.distanceToCenterSqr(pos)));
    }

    public Optional<TemporalSite> findContaining(BlockPos pos) {
        return inChunk(ChunkPos.containing(pos)).stream().filter(site -> site.contains(pos)).findFirst();
    }

    public Collection<TemporalSite> inChunk(ChunkPos chunkPos) {
        Set<UUID> ids = sitesByChunk.get(chunkPos.pack());
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().map(sites::get).filter(java.util.Objects::nonNull).toList();
    }

    public Collection<TemporalSite> all() {
        return List.copyOf(sites.values());
    }

    public boolean hasTransitions() {
        return sites.values().stream().anyMatch(TemporalSite::isTransitioning);
    }

    public int size() {
        return sites.size();
    }

    public List<FractureShrinePlacement> fractureShrines() {
        return fractureShrines;
    }

    public void setFractureShrines(List<FractureShrinePlacement> placements) {
        fractureShrines = List.copyOf(placements);
        setDirty();
    }

    public Optional<BlockPos> archiveOrigin() {
        return archiveOrigin;
    }

    public void setArchiveOrigin(BlockPos origin) {
        archiveOrigin = Optional.of(origin.immutable());
        setDirty();
    }

    private static TemporalSiteSavedData fromCodec(
            int schemaRevision,
            List<TemporalSite> decodedSites,
            List<FractureShrinePlacement> decodedShrines,
            Optional<BlockPos> decodedArchiveOrigin) {
        TemporalSiteSavedData data = new TemporalSiteSavedData();
        if (schemaRevision != SCHEMA_REVISION) {
            Yesterglass.LOGGER.warn("Loaded temporal site schema {}; current schema is {}", schemaRevision, SCHEMA_REVISION);
        }
        decodedSites.forEach(site -> {
            data.sites.putIfAbsent(site.siteId(), site);
            data.index(site);
        });
        data.fractureShrines = List.copyOf(decodedShrines);
        data.archiveOrigin = decodedArchiveOrigin.map(BlockPos::immutable);
        return data;
    }

    private void index(TemporalSite site) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        BlockPos[] corners = {
                definition.worldPosition(site.origin(), BlockPos.ZERO, site.rotation()),
                definition.worldPosition(site.origin(), new BlockPos(definition.sizeX() - 1, 0, 0), site.rotation()),
                definition.worldPosition(site.origin(), new BlockPos(0, 0, definition.sizeZ() - 1), site.rotation()),
                definition.worldPosition(
                        site.origin(),
                        new BlockPos(definition.sizeX() - 1, 0, definition.sizeZ() - 1),
                        site.rotation())
        };
        int minX = java.util.Arrays.stream(corners).mapToInt(BlockPos::getX).min().orElse(site.origin().getX());
        int maxX = java.util.Arrays.stream(corners).mapToInt(BlockPos::getX).max().orElse(site.origin().getX());
        int minZ = java.util.Arrays.stream(corners).mapToInt(BlockPos::getZ).min().orElse(site.origin().getZ());
        int maxZ = java.util.Arrays.stream(corners).mapToInt(BlockPos::getZ).max().orElse(site.origin().getZ());
        ChunkPos min = ChunkPos.containing(new BlockPos(minX, site.origin().getY(), minZ));
        ChunkPos max = ChunkPos.containing(new BlockPos(maxX, site.origin().getY(), maxZ));
        for (int chunkX = min.x(); chunkX <= max.x(); chunkX++) {
            for (int chunkZ = min.z(); chunkZ <= max.z(); chunkZ++) {
                sitesByChunk.computeIfAbsent(ChunkPos.pack(chunkX, chunkZ), ignored -> new java.util.LinkedHashSet<>())
                        .add(site.siteId());
            }
        }
    }
}
