package com.nightbeam.tbos.client;

import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public final class ClientTransitionTracker {
    private static final int SEGMENT_COUNT = 64;
    private static final int MAX_ACTIVE_TRANSITIONS = 8;
    private static final Map<UUID, ActiveTransition> ACTIVE = new LinkedHashMap<>();

    private ClientTransitionTracker() {
    }

    public static void begin(BeginTransitionPayload payload) {
        if (!ACTIVE.containsKey(payload.siteId()) && ACTIVE.size() >= MAX_ACTIVE_TRANSITIONS) {
            Iterator<UUID> oldest = ACTIVE.keySet().iterator();
            if (oldest.hasNext()) {
                ACTIVE.remove(oldest.next());
            }
        }
        ACTIVE.put(payload.siteId(), new ActiveTransition(payload));
    }

    public static void snapshot(SiteSnapshotPayload payload) {
        if (payload.state().isStable()) {
            ACTIVE.remove(payload.siteId());
            return;
        }
        if (payload.transitionDurationTicks() > 0) {
            begin(new BeginTransitionPayload(
                    payload.siteId(),
                    payload.definitionId(),
                    payload.origin(),
                    payload.center(),
                    payload.rotation(),
                    payload.state().targetStableState(),
                    payload.progressFlags(),
                    payload.transitionStartTick(),
                    payload.transitionDurationTicks(),
                    payload.effectSeed()));
        }
    }

    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        long gameTick = level.getGameTime();
        Iterator<ActiveTransition> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            ActiveTransition transition = iterator.next();
            double progress = (gameTick - transition.startTick) / (double) transition.durationTicks;
            progress = Math.max(0.0D, Math.min(1.0D, progress));
            transition.spawnNewlyActivatedSegments(level, progress);
            if (progress >= 1.0D) {
                iterator.remove();
            }
        }
    }

    private static final class ActiveTransition {
        private final BlockPos center;
        private final boolean toRemembered;
        private final long startTick;
        private final int durationTicks;
        private final long seed;
        private final boolean[] activated = new boolean[SEGMENT_COUNT];

        private ActiveTransition(BeginTransitionPayload payload) {
            this.center = payload.center();
            this.toRemembered = payload.targetState() == com.nightbeam.tbos.site.TemporalState.REMEMBERED;
            this.startTick = payload.startTick();
            this.durationTicks = payload.durationTicks();
            this.seed = payload.effectSeed();
        }

        private void spawnNewlyActivatedSegments(ClientLevel level, double progress) {
            int quality = YesterglassClientConfig.EFFECT_QUALITY.getAsInt();
            int particlesPerSegment = switch (quality) {
                case 0 -> 1;
                case 1 -> 2;
                case 2 -> 4;
                default -> 8;
            };
            boolean reducedMotion = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean();

            for (int index = 0; index < SEGMENT_COUNT; index++) {
                if (activated[index] || progress < activationThreshold(index)) {
                    continue;
                }
                activated[index] = true;
                spawnSegment(level, index, particlesPerSegment, reducedMotion);
            }
        }

        private double activationThreshold(int index) {
            int ring = index / 8;
            long mixed = seed ^ (index * 0x9E3779B97F4A7C15L);
            double noise = (((mixed >>> 24) & 255L) / 255.0D - 0.5D) * 0.06D;
            return 0.04D + ring / 7.0D * 0.78D + noise;
        }

        private void spawnSegment(ClientLevel level, int index, int count, boolean reducedMotion) {
            int ring = index / 8;
            int spoke = index % 8;
            double angle = spoke * Math.PI / 4.0D + ring * 0.17D;
            double radius = 1.0D + ring * 0.95D;
            double x = center.getX() + 0.5D + Math.cos(angle) * radius;
            double y = center.getY() - 0.8D + (index % 4) * 0.8D;
            double z = center.getZ() + 0.5D + Math.sin(angle) * radius;
            ParticleOptions primary = toRemembered ? ParticleTypes.END_ROD : ParticleTypes.ASH;
            ParticleOptions secondary = ParticleTypes.REVERSE_PORTAL;

            for (int particle = 0; particle < count; particle++) {
                double jitter = (particle - (count - 1) * 0.5D) * 0.045D;
                double velocity = reducedMotion ? 0.0D : (toRemembered ? 0.015D : -0.012D);
                level.addParticle(primary, x + jitter, y + jitter * 0.5D, z - jitter, 0.0D, velocity, 0.0D);
                if (!reducedMotion && particle % 2 == 0) {
                    level.addParticle(secondary, x, y, z, Math.cos(angle) * 0.01D, 0.0D, Math.sin(angle) * 0.01D);
                }
            }
        }
    }
}
