package com.nightbeam.tbos.run;

import java.util.List;

/**
 * Code-native floor theme catalog keyed by {@link ArchiveFloorPresentation#nameIndex(long)}.
 * Echo cycles reuse the same theme identity.
 */
public enum ArchiveFloorTheme {
    PARALLAX_WAKE(
            ArchiveEnemyKind.WAKE_CUTTER,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.SHARD_DRIFTER, 4)),
            ArchiveThemeHazard.PARALLAX_PANEL),
    STARLESS_GALLERY(
            ArchiveEnemyKind.NULL_PORTRAIT,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.GALLERY_MOTH, 3)),
            ArchiveThemeHazard.LIGHT_DUST),
    MERIDIAN_DESCENT(
            ArchiveEnemyKind.GNOMON_KNIGHT,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.ARMILLARY_SCOUT, 3)),
            ArchiveThemeHazard.COLLAPSING_TILE),
    CHOIR_OF_DUST(
            ArchiveEnemyKind.DUST_CANTORILE,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.ASH_CHORISTER, 3)),
            ArchiveThemeHazard.BRITTLE_ASH),
    GLASSBOUND_VAULT(
            ArchiveEnemyKind.PRISM_STALKER,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.SHARDLING_SWARM, 3)),
            ArchiveThemeHazard.SHATTER_PANE),
    HOLLOW_CATALOGUE(
            ArchiveEnemyKind.INDEX_WIGHT,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.SHELF_CRAWLER, 3)),
            ArchiveThemeHazard.FALSE_SHELF),
    CANTORS_LABYRINTH(
            ArchiveEnemyKind.HOUR_CANTOR,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.METRONOME_HOUND, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.LABYRINTH_USHER, 3)),
            ArchiveThemeHazard.RESONANT_PLATE),
    UNWRITTEN_HOUR(
            ArchiveEnemyKind.HOUR_HAND_WRAITH,
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.BLANK_CHRONIST, 4)),
            ArchiveThemeHazard.INK_POOL);

    private final ArchiveEnemyKind bossKind;
    private final List<ArchiveDungeonRules.EnemyWeight> exclusiveWeights;
    private final ArchiveThemeHazard hazard;

    ArchiveFloorTheme(
            ArchiveEnemyKind bossKind,
            List<ArchiveDungeonRules.EnemyWeight> exclusiveWeights,
            ArchiveThemeHazard hazard) {
        this.bossKind = bossKind;
        this.exclusiveWeights = List.copyOf(exclusiveWeights);
        this.hazard = hazard;
    }

    public List<ArchiveDungeonRules.EnemyWeight> exclusiveWeights() {
        return exclusiveWeights;
    }

    public ArchiveEnemyKind bossKind() {
        return bossKind;
    }

    public ArchiveThemeHazard hazard() {
        return hazard;
    }

    public static ArchiveFloorTheme of(long floorIndex) {
        return values()[ArchiveFloorPresentation.nameIndex(floorIndex)];
    }
}
