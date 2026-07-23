package com.nightbeam.tbos.site;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

final class ChoirPlaybackTracker {
    private static final int START_DELAY_TICKS = 10;
    private static final int BEAT_INTERVAL_TICKS = 18;
    private static final Map<ResourceKey<Level>, Map<UUID, Playback>> ACTIVE = new ConcurrentHashMap<>();

    private ChoirPlaybackTracker() {
    }

    static void restart(ServerLevel level, TemporalSite site) {
        if (!canPlay(site)) {
            stop(level, site, true);
            return;
        }
        TemporalSiteManager.clearChoirImprints(level, site);
        ACTIVE.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(site.siteId(), new Playback(level.getGameTime() + START_DELAY_TICKS));
    }

    static void startIfAbsent(ServerLevel level, TemporalSite site) {
        if (!canPlay(site)) {
            return;
        }
        ACTIVE.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .computeIfAbsent(site.siteId(), ignored -> new Playback(level.getGameTime() + START_DELAY_TICKS));
    }

    static void stop(ServerLevel level, TemporalSite site, boolean clearVisuals) {
        Map<UUID, Playback> playbacks = ACTIVE.get(level.dimension());
        if (playbacks != null) {
            playbacks.remove(site.siteId());
            if (playbacks.isEmpty()) {
                ACTIVE.remove(level.dimension());
            }
        }
        if (clearVisuals && TemporalSiteManager.isSiteLoaded(level, site)) {
            TemporalSiteManager.clearChoirImprints(level, site);
        }
    }

    static void tick(MinecraftServer server) {
        for (ResourceKey<Level> dimension : java.util.List.copyOf(ACTIVE.keySet())) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) {
                ACTIVE.remove(dimension);
                continue;
            }
            Map<UUID, Playback> playbacks = ACTIVE.get(dimension);
            if (playbacks == null) {
                continue;
            }
            Iterator<Map.Entry<UUID, Playback>> iterator = playbacks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Playback> entry = iterator.next();
                TemporalSite site = TemporalSiteManager.data(level).find(entry.getKey()).orElse(null);
                if (site == null || !canPlay(site) || !TemporalSiteManager.isSiteLoaded(level, site)) {
                    iterator.remove();
                    continue;
                }
                Playback playback = entry.getValue();
                long elapsed = level.getGameTime() - playback.startTick;
                if (elapsed < 0L) {
                    continue;
                }
                int beat = (int) (elapsed / BEAT_INTERVAL_TICKS);
                if (beat >= ChoirHoursPuzzle.sequence().size()) {
                    TemporalSiteManager.clearChoirImprints(level, site);
                    iterator.remove();
                    continue;
                }
                if (beat != playback.lastBeat) {
                    playback.lastBeat = beat;
                    TemporalSiteManager.showChoirBeat(level, site, ChoirHoursPuzzle.sequence().get(beat));
                }
            }
            if (playbacks.isEmpty()) {
                ACTIVE.remove(dimension);
            }
        }
    }

    static void clear() {
        ACTIVE.clear();
    }

    static boolean isActive(ResourceKey<Level> dimension, UUID siteId) {
        Map<UUID, Playback> playbacks = ACTIVE.get(dimension);
        return playbacks != null && playbacks.containsKey(siteId);
    }

    private static boolean canPlay(TemporalSite site) {
        return site.definitionId().equals(BuiltInTemporalSites.CHOIR_OF_HOURS_ID)
                && site.state() == TemporalState.REMEMBERED
                && !site.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE);
    }

    private static final class Playback {
        private final long startTick;
        private int lastBeat = -1;

        private Playback(long startTick) {
            this.startTick = startTick;
        }
    }
}
