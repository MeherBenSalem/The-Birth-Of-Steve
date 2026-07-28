package com.nightbeam.tbos.entity;

import com.nightbeam.tbos.run.ArchiveEnemyKind;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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

    public boolean bossEligible() {
        return switch (this) {
            case WAKE_CUTTER, NULL_PORTRAIT, GNOMON_KNIGHT, DUST_CANTORILE,
                    PRISM_STALKER, INDEX_WIGHT, HOUR_HAND_WRAITH -> true;
            default -> false;
        };
    }

    /**
     * The particle thrown during WINDUP.
     *
     * <p>All sixteen shared one enchantment-glyph burst, which made every
     * signature move look the same in the half-second a player has to react to
     * it. The material each kind is made of is the readable cue: ash for the
     * Choir's pair, sparks for the Vault's crystals, ink for the Unwritten.
     */
    public ParticleOptions telegraph() {
        return switch (this) {
            case SHARD_DRIFTER -> ParticleTypes.REVERSE_PORTAL;
            case WAKE_CUTTER -> ParticleTypes.CRIT;
            case NULL_PORTRAIT -> ParticleTypes.SMOKE;
            case GALLERY_MOTH -> ParticleTypes.ASH;
            case GNOMON_KNIGHT -> ParticleTypes.ELECTRIC_SPARK;
            case ARMILLARY_SCOUT -> ParticleTypes.END_ROD;
            case DUST_CANTORILE, ASH_CHORISTER -> ParticleTypes.WHITE_ASH;
            case PRISM_STALKER, SHARDLING_SWARM -> ParticleTypes.GLOW;
            case INDEX_WIGHT -> ParticleTypes.WITCH;
            case SHELF_CRAWLER -> ParticleTypes.SCRAPE;
            case METRONOME_HOUND -> ParticleTypes.WAX_ON;
            case LABYRINTH_USHER -> ParticleTypes.SOUL;
            case BLANK_CHRONIST -> ParticleTypes.SQUID_INK;
            case HOUR_HAND_WRAITH -> ParticleTypes.SOUL_FIRE_FLAME;
        };
    }

    // Vanilla sound events on purpose. The five hand-built Archive creatures own
    // registered tbos: events backed by remapped vanilla samples, and minting
    // forty-eight more would claim a sound design that does not exist yet.
    // These are audibly placeholder, which is what CLAUDE.md asks for.
    public SoundEvent ambientSound() {
        return switch (this) {
            case SHARD_DRIFTER, SHARDLING_SWARM, PRISM_STALKER -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case WAKE_CUTTER, HOUR_HAND_WRAITH -> SoundEvents.PHANTOM_AMBIENT;
            case NULL_PORTRAIT, BLANK_CHRONIST -> SoundEvents.PAINTING_PLACE;
            case GALLERY_MOTH -> SoundEvents.BAT_AMBIENT;
            case GNOMON_KNIGHT -> SoundEvents.IRON_GOLEM_STEP;
            case ARMILLARY_SCOUT -> SoundEvents.BEACON_AMBIENT;
            case DUST_CANTORILE, ASH_CHORISTER -> SoundEvents.VEX_AMBIENT;
            case INDEX_WIGHT -> SoundEvents.BOOK_PAGE_TURN;
            case SHELF_CRAWLER -> SoundEvents.SPIDER_AMBIENT;
            case METRONOME_HOUND -> SoundEvents.COMPARATOR_CLICK;
            case LABYRINTH_USHER -> SoundEvents.PORTAL_AMBIENT;
        };
    }

    public SoundEvent hurtSound() {
        return switch (this) {
            case SHARD_DRIFTER, SHARDLING_SWARM, PRISM_STALKER -> SoundEvents.AMETHYST_BLOCK_BREAK;
            case GNOMON_KNIGHT, ARMILLARY_SCOUT, METRONOME_HOUND -> SoundEvents.IRON_GOLEM_HURT;
            case NULL_PORTRAIT, INDEX_WIGHT, BLANK_CHRONIST -> SoundEvents.BOOK_PUT;
            case GALLERY_MOTH, SHELF_CRAWLER -> SoundEvents.SPIDER_HURT;
            default -> SoundEvents.VEX_HURT;
        };
    }

    public SoundEvent deathSound() {
        return switch (this) {
            case SHARD_DRIFTER, SHARDLING_SWARM, PRISM_STALKER -> SoundEvents.GLASS_BREAK;
            case GNOMON_KNIGHT, ARMILLARY_SCOUT, METRONOME_HOUND -> SoundEvents.IRON_GOLEM_DEATH;
            case GALLERY_MOTH, SHELF_CRAWLER -> SoundEvents.SPIDER_DEATH;
            default -> SoundEvents.VEX_DEATH;
        };
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
