package com.nightbeam.tbos.run;

import java.util.List;

/**
 * Code-native floor theme catalog keyed by {@link ArchiveFloorPresentation#nameIndex(long)}.
 * Echo cycles reuse the same theme identity.
 */
public enum ArchiveFloorTheme {
    PARALLAX_WAKE(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.SHARD_DRIFTER, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.WAKE_CUTTER, 3)),
            ArchiveThemeHazard.PARALLAX_PANEL),
    STARLESS_GALLERY(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.NULL_PORTRAIT, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.GALLERY_MOTH, 3)),
            ArchiveThemeHazard.LIGHT_DUST),
    MERIDIAN_DESCENT(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.GNOMON_KNIGHT, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.ARMILLARY_SCOUT, 3)),
            ArchiveThemeHazard.COLLAPSING_TILE),
    CHOIR_OF_DUST(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.DUST_CANTORILE, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.ASH_CHORISTER, 3)),
            ArchiveThemeHazard.BRITTLE_ASH),
    GLASSBOUND_VAULT(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.PRISM_STALKER, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.SHARDLING_SWARM, 3)),
            ArchiveThemeHazard.SHATTER_PANE),
    HOLLOW_CATALOGUE(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.INDEX_WIGHT, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.SHELF_CRAWLER, 3)),
            ArchiveThemeHazard.FALSE_SHELF),
    CANTORS_LABYRINTH(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.METRONOME_HOUND, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.LABYRINTH_USHER, 3)),
            ArchiveThemeHazard.RESONANT_PLATE),
    UNWRITTEN_HOUR(
            List.of(
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.BLANK_CHRONIST, 4),
                    new ArchiveDungeonRules.EnemyWeight(ArchiveEnemyKind.HOUR_HAND_WRAITH, 3)),
            ArchiveThemeHazard.INK_POOL);

    private final List<ArchiveDungeonRules.EnemyWeight> exclusiveWeights;
    private final ArchiveThemeHazard hazard;

    ArchiveFloorTheme(List<ArchiveDungeonRules.EnemyWeight> exclusiveWeights, ArchiveThemeHazard hazard) {
        this.exclusiveWeights = List.copyOf(exclusiveWeights);
        this.hazard = hazard;
    }

    public List<ArchiveDungeonRules.EnemyWeight> exclusiveWeights() {
        return exclusiveWeights;
    }

    public ArchiveThemeHazard hazard() {
        return hazard;
    }

    public static ArchiveFloorTheme of(long floorIndex) {
        return values()[ArchiveFloorPresentation.nameIndex(floorIndex)];
    }
}
