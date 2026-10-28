package com.nightbeam.tbos.expedition;

import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.rpg.RpgConfig;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Recollections: a Memory Key used inside any structure, vanilla or modded, opens
 * an expedition seeded from that structure. Structures tagged
 * {@code #tbos:unrecollectable} are skipped.
 */
public final class Recollections {
    public static final String EXCLUDED_TAG = "tbos:unrecollectable";

    private Recollections() {
    }

    public static Optional<RpgCompat.StructureHit> find(ServerLevel level, BlockPos pos) {
        if (!RpgConfig.get().expeditions.recollections) {
            return Optional.empty();
        }
        return RpgCompat.structureAt(level, pos)
                .filter(hit -> !RpgCompat.structureTagged(level, hit.id(), EXCLUDED_TAG));
    }

    /** What a player's Codex remembers about a recollected structure. */
    public static String memoryKey(RpgCompat.StructureHit hit) {
        return hit.id() + "@" + hit.startKey();
    }

    /** A readable name: a translation when the pack has one, otherwise the id turned into words. */
    public static Component name(String id) {
        if (id == null || id.isEmpty()) {
            return Component.translatable("expedition.tbos.place.archive");
        }
        String key = "recollection.tbos.structure." + id.replace(':', '.');
        return Component.translatableWithFallback(key, humanize(id));
    }

    public static String humanize(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        int slash = path.lastIndexOf('/');
        if (slash >= 0) {
            path = path.substring(slash + 1);
        }
        StringBuilder out = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
