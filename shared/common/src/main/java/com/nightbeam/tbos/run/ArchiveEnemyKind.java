package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** Original Echoes of the Past enemy archetypes accepted by encounter-pool config. */
public enum ArchiveEnemyKind {
    PARALLAX_WRAITH(Yesterglass.MOD_ID, "parallax_wraith"),
    MERIDIAN_SENTINEL(Yesterglass.MOD_ID, "meridian_sentinel"),
    HOUR_CANTOR(Yesterglass.MOD_ID, "hour_cantor"),
    HUSK("minecraft", "husk"),
    SKELETON("minecraft", "skeleton"),
    STRAY("minecraft", "stray"),
    CAVE_SPIDER("minecraft", "cave_spider"),
    SILVERFISH("minecraft", "silverfish"),
    VINDICATOR("minecraft", "vindicator"),
    EVOKER("minecraft", "evoker"),
    RAVAGER("minecraft", "ravager"),
    MEMORY_LEECH(Yesterglass.MOD_ID, "memory_leech"),
    // Theme exclusives — append before LENSWARD; drop seeds use ordinal() and Lensward stays last.
    SHARD_DRIFTER(Yesterglass.MOD_ID, "shard_drifter"),
    WAKE_CUTTER(Yesterglass.MOD_ID, "wake_cutter"),
    NULL_PORTRAIT(Yesterglass.MOD_ID, "null_portrait"),
    GALLERY_MOTH(Yesterglass.MOD_ID, "gallery_moth"),
    GNOMON_KNIGHT(Yesterglass.MOD_ID, "gnomon_knight"),
    ARMILLARY_SCOUT(Yesterglass.MOD_ID, "armillary_scout"),
    DUST_CANTORILE(Yesterglass.MOD_ID, "dust_cantorile"),
    ASH_CHORISTER(Yesterglass.MOD_ID, "ash_chorister"),
    PRISM_STALKER(Yesterglass.MOD_ID, "prism_stalker"),
    SHARDLING_SWARM(Yesterglass.MOD_ID, "shardling_swarm"),
    INDEX_WIGHT(Yesterglass.MOD_ID, "index_wight"),
    SHELF_CRAWLER(Yesterglass.MOD_ID, "shelf_crawler"),
    METRONOME_HOUND(Yesterglass.MOD_ID, "metronome_hound"),
    LABYRINTH_USHER(Yesterglass.MOD_ID, "labyrinth_usher"),
    BLANK_CHRONIST(Yesterglass.MOD_ID, "blank_chronist"),
    HOUR_HAND_WRAITH(Yesterglass.MOD_ID, "hour_hand_wraith"),
    // Chamber bruiser, placed before LENSWARD: the `lensward_contract` GameTest
    // asserts Lensward is the last constant, because rollEnemyDrop seeds on
    // ordinal(). New kinds go here, above it, never after.
    MINOTAUR(Yesterglass.MOD_ID, "minotaur"),
    LENSWARD(Yesterglass.MOD_ID, "lensward");

    private final Identifier id;

    ArchiveEnemyKind(String namespace, String path) {
        id = Identifier.fromNamespaceAndPath(namespace, path);
    }

    public Identifier id() {
        return id;
    }

    public static Optional<ArchiveEnemyKind> parse(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return java.util.Arrays.stream(values())
                .filter(kind -> kind.id.toString().equals(normalized)
                        || kind.name().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }
}
