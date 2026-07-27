package com.nightbeam.tbos.entity;

import com.nightbeam.tbos.run.ArchiveEnemyKind;
import net.minecraft.world.entity.EntityType;

/** Maps each theme-exclusive entity type onto its signature ability and silhouette. */
public enum ThemeExclusiveKind {
    SHARD_DRIFTER(ArchiveEnemyKind.SHARD_DRIFTER, Silhouette.ORBIT_SHARDS, 0.7F, 1.4F),
    WAKE_CUTTER(ArchiveEnemyKind.WAKE_CUTTER, Silhouette.SCYTHE, 0.7F, 1.8F),
    NULL_PORTRAIT(ArchiveEnemyKind.NULL_PORTRAIT, Silhouette.FLAT_FRAME, 0.8F, 1.9F),
    GALLERY_MOTH(ArchiveEnemyKind.GALLERY_MOTH, Silhouette.WINGED, 0.6F, 0.7F),
    GNOMON_KNIGHT(ArchiveEnemyKind.GNOMON_KNIGHT, Silhouette.HEAVY_KNIGHT, 0.8F, 2.1F),
    ARMILLARY_SCOUT(ArchiveEnemyKind.ARMILLARY_SCOUT, Silhouette.RINGED, 0.6F, 1.2F),
    DUST_CANTORILE(ArchiveEnemyKind.DUST_CANTORILE, Silhouette.ROBED, 0.6F, 1.9F),
    ASH_CHORISTER(ArchiveEnemyKind.ASH_CHORISTER, Silhouette.SPLIT_CORE, 0.55F, 1.3F),
    PRISM_STALKER(ArchiveEnemyKind.PRISM_STALKER, Silhouette.PRISM, 0.7F, 1.7F),
    SHARDLING_SWARM(ArchiveEnemyKind.SHARDLING_SWARM, Silhouette.SWARM, 0.5F, 0.6F),
    INDEX_WIGHT(ArchiveEnemyKind.INDEX_WIGHT, Silhouette.PAGE, 0.6F, 1.8F),
    SHELF_CRAWLER(ArchiveEnemyKind.SHELF_CRAWLER, Silhouette.CLINGER, 0.9F, 0.5F),
    METRONOME_HOUND(ArchiveEnemyKind.METRONOME_HOUND, Silhouette.HOUND, 0.8F, 0.9F),
    LABYRINTH_USHER(ArchiveEnemyKind.LABYRINTH_USHER, Silhouette.USHER, 0.7F, 2.0F),
    BLANK_CHRONIST(ArchiveEnemyKind.BLANK_CHRONIST, Silhouette.BLANK, 0.65F, 1.85F),
    HOUR_HAND_WRAITH(ArchiveEnemyKind.HOUR_HAND_WRAITH, Silhouette.LONG_ARM, 0.7F, 2.2F);

    public enum Silhouette {
        ORBIT_SHARDS,
        SCYTHE,
        FLAT_FRAME,
        WINGED,
        HEAVY_KNIGHT,
        RINGED,
        ROBED,
        SPLIT_CORE,
        PRISM,
        SWARM,
        PAGE,
        CLINGER,
        HOUND,
        USHER,
        BLANK,
        LONG_ARM
    }

    private final ArchiveEnemyKind enemyKind;
    private final Silhouette silhouette;
    private final float width;
    private final float height;

    ThemeExclusiveKind(ArchiveEnemyKind enemyKind, Silhouette silhouette, float width, float height) {
        this.enemyKind = enemyKind;
        this.silhouette = silhouette;
        this.width = width;
        this.height = height;
    }

    public ArchiveEnemyKind enemyKind() {
        return enemyKind;
    }

    public Silhouette silhouette() {
        return silhouette;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public String texturePath() {
        return enemyKind.id().getPath();
    }

    public static ThemeExclusiveKind of(EntityType<?> type) {
        for (ThemeExclusiveKind kind : values()) {
            if (kind.enemyKind.id().getPath().equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(type)
                    .getPath())) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown theme exclusive entity type: " + type);
    }
}
