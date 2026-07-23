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
    RAVAGER("minecraft", "ravager");

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
