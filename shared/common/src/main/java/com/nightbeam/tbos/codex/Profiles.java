package com.nightbeam.tbos.codex;

import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.rpg.JsonStore;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Loads, caches and saves {@link PlayerProfile}s as
 * {@code <world>/data/tbos/codex/<uuid>.json}. A profile is read on first use, kept
 * while the server runs, and written when its player leaves, every five minutes
 * if it changed, and when the server stops.
 */
public final class Profiles {
    private static final Map<UUID, PlayerProfile> CACHE = new HashMap<>();
    private static final int AUTOSAVE_TICKS = 20 * 60 * 5;

    private Profiles() {
    }

    public static PlayerProfile get(ServerPlayer player) {
        PlayerProfile profile = get(player.level().getServer(), player.getUUID());
        String name = RpgCompat.playerName(player);
        if (!name.equals(profile.name)) {
            profile.name = name;
            profile.dirty = true;
        }
        return profile;
    }

    public static PlayerProfile get(MinecraftServer server, UUID id) {
        return CACHE.computeIfAbsent(id, key -> PlayerProfile.fromJson(key, JsonStore.read(file(server, key))));
    }

    private static Path file(MinecraftServer server, UUID id) {
        return JsonStore.directory(server, "codex", id + ".json");
    }

    public static void save(MinecraftServer server, UUID id) {
        PlayerProfile profile = CACHE.get(id);
        if (profile != null && profile.dirty) {
            JsonStore.write(file(server, id), profile.toJson());
            profile.dirty = false;
        }
    }

    public static void unload(MinecraftServer server, UUID id) {
        save(server, id);
        CACHE.remove(id);
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % AUTOSAVE_TICKS == 0) {
            saveAll(server);
        }
    }

    public static void saveAll(MinecraftServer server) {
        for (UUID id : CACHE.keySet()) {
            save(server, id);
        }
    }

    public static void clear() {
        CACHE.clear();
    }
}
